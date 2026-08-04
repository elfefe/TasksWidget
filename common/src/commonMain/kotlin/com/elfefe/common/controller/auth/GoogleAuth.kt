package com.elfefe.common.controller.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elfefe.common.controller.log
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.awt.Desktop
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import kotlin.concurrent.thread

/**
 * Connexion Google **sans mot de passe à saisir**.
 *
 * L'application n'a jamais à connaître d'identifiants : elle ouvre le
 * navigateur, où la session Google est déjà ouverte, l'utilisateur accorde
 * l'accès d'un clic, et Google renvoie un code sur un serveur local éphémère.
 * Ce code est échangé contre un jeton de rafraîchissement, conservé chiffré
 * ([SecretStore]) — après quoi les lancements suivants se connectent seuls, en
 * silence.
 *
 * C'est le flux recommandé pour une application de bureau (RFC 8252) :
 * *authorization code* + **PKCE**, et redirection sur `127.0.0.1` avec un port
 * tiré au lancement. L'ancien code du projet retenait le port 8080 en dur et un
 * serveur Ktor jamais arrêté ; un port déjà pris suffisait à tout bloquer.
 */
object GoogleAuth {

    private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    private const val REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke"

    /**
     * Portées demandées : l'identité, et l'accès aux données de l'utilisateur
     * pour la synchronisation. L'ancien code réclamait `cloud-platform`, soit
     * les clés du projet entier — hors de proportion pour un widget de tâches.
     */
    private val SCOPES = listOf(
        "openid",
        "profile",
        "email",
        "https://www.googleapis.com/auth/datastore"
    )

    /**
     * Délai laissé à l'utilisateur pour accorder l'accès dans son navigateur.
     *
     * Trois minutes ne suffisaient pas : tant que le projet n'est pas validé par
     * Google, l'accord passe par un écran d'avertissement, un dépliement des
     * paramètres avancés et un second lien — sans compter une éventuelle
     * saisie de mot de passe Google. Le flux expirait en cours de route.
     */
    private const val CONSENT_TIMEOUT_MS = 10 * 60 * 1000

    sealed interface State {
        object SignedOut : State
        object Connecting : State
        data class SignedIn(val email: String, val name: String, val picture: String) : State
        data class Failed(val reason: String) : State
    }

    var state by mutableStateOf<State>(State.SignedOut)
        private set

    /** Jeton d'accès courant et son échéance, gardés en mémoire seulement. */
    private var accessToken: String? = null
    private var accessTokenExpiry: Long = 0

    private val gson = Gson()
    private val http: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
    }

    val isSignedIn: Boolean get() = state is State.SignedIn

    /**
     * Reprend la session sans rien demander, s'il reste un jeton de
     * rafraîchissement. À appeler au démarrage : c'est ce qui rend la connexion
     * « automatique » une fois le premier accord donné.
     */
    fun restore() {
        val refreshToken = SecretStore.load() ?: return
        state = State.Connecting
        thread(isDaemon = true, name = "google-auth-restore") {
            if (refresh(refreshToken) == null) {
                // Jeton révoqué ou expiré : on repart proprement d'un état
                // déconnecté plutôt que d'afficher une erreur inexplicable.
                SecretStore.clear()
                state = State.SignedOut
            }
        }
    }

    /**
     * Ouvre le navigateur pour obtenir l'accord de l'utilisateur. Sans effet si
     * les identifiants du client OAuth ne sont pas embarqués.
     */
    fun signIn() {
        val credentials = OAuthCredentials.current
        if (credentials == null) {
            state = State.Failed("Identifiants OAuth absents de cette version")
            log("GoogleAuth: aucun client OAuth embarqué, connexion impossible")
            return
        }
        if (state is State.Connecting) return
        state = State.Connecting

        thread(isDaemon = true, name = "google-auth-signin") {
            runCatching { authorize(credentials) }
                .onFailure {
                    log("GoogleAuth: connexion impossible\n" + it.stackTraceToString())
                    state = State.Failed("Connexion impossible")
                }
        }
    }

    /** Oublie le compte et invalide le jeton côté Google. */
    fun signOut() {
        val refreshToken = SecretStore.load()
        SecretStore.clear()
        accessToken = null
        accessTokenExpiry = 0
        state = State.SignedOut
        if (refreshToken.isNullOrBlank()) return
        thread(isDaemon = true, name = "google-auth-revoke") {
            runCatching {
                http.send(
                    HttpRequest.newBuilder(URI.create("$REVOKE_ENDPOINT?token=$refreshToken"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.discarding()
                )
            }.onFailure { log("GoogleAuth: révocation impossible\n" + it.stackTraceToString()) }
        }
    }

    /**
     * Jeton d'accès valide, rafraîchi si besoin. Renvoie `null` si personne
     * n'est connecté — l'appelant doit alors se passer du réseau.
     */
    @Synchronized
    fun accessToken(): String? {
        val token = accessToken
        // Une minute de marge : un jeton qui expire pendant le trajet de la
        // requête produirait un 401 inexplicable.
        if (token != null && System.currentTimeMillis() < accessTokenExpiry - 60_000) return token
        val refreshToken = SecretStore.load() ?: return null
        return refresh(refreshToken)
    }

    /** Déroule l'accord dans le navigateur et échange le code obtenu. */
    private fun authorize(credentials: OAuthCredentials) {
        val verifier = randomUrlSafe(64)
        val challenge = sha256Base64Url(verifier)
        val expectedState = randomUrlSafe(24)

        // Port tiré par le système : deux instances peuvent se connecter en
        // même temps, et aucun service tiers ne peut nous en priver.
        ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { server ->
            val redirectUri = "http://127.0.0.1:${server.localPort}"
            val url = buildString {
                append(AUTH_ENDPOINT)
                append("?response_type=code")
                append("&client_id=").append(encode(credentials.clientId))
                append("&redirect_uri=").append(encode(redirectUri))
                append("&scope=").append(encode(SCOPES.joinToString(" ")))
                append("&state=").append(expectedState)
                append("&code_challenge=").append(challenge)
                append("&code_challenge_method=S256")
                // Indispensable pour obtenir un refresh_token : sans cela,
                // Google n'en délivre pas et il faudrait re-consentir chaque
                // fois — exactement ce qu'on veut éviter.
                append("&access_type=offline")
                append("&prompt=consent")
            }

            openBrowser(url)

            server.soTimeout = CONSENT_TIMEOUT_MS
            val result = awaitRedirect(server)
            if (result == null) {
                state = State.Failed("Aucune réponse du navigateur")
                return
            }
            if (result.state != expectedState) {
                // Garde-fou contre une redirection forgée par un tiers.
                state = State.Failed("Réponse d'authentification inattendue")
                log("GoogleAuth: state inattendu, échange abandonné")
                return
            }
            val code = result.code
            if (code == null) {
                state = State.Failed(result.error ?: "Accès refusé")
                return
            }
            exchangeCode(credentials, code, verifier, redirectUri)
        }
    }

    private class RedirectResult(val code: String?, val state: String?, val error: String?)

    /**
     * Attend l'unique requête que Google adresse au serveur local, en extrait le
     * code, et répond une page qui invite à revenir dans l'application.
     */
    private fun awaitRedirect(server: ServerSocket): RedirectResult? = runCatching {
        server.accept().use { socket ->
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return null
            val target = requestLine.split(' ').getOrNull(1).orEmpty()
            val query = target.substringAfter('?', "")
            val params = query.split('&')
                .mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
                .associate { decode(it[0]) to decode(it[1]) }

            respond(socket.getOutputStream(), params["error"] == null)
            RedirectResult(params["code"], params["state"], params["error"])
        }
    }.onFailure { log("GoogleAuth: attente de la redirection interrompue\n" + it.stackTraceToString()) }
        .getOrNull()

    private fun respond(output: OutputStream, success: Boolean) {
        val title = if (success) "Connexion réussie" else "Connexion refusée"
        val message =
            if (success) "Vous pouvez fermer cet onglet et revenir à TasksWidget."
            else "Aucun accès n'a été accordé. Vous pouvez fermer cet onglet."
        val body = """
            <!doctype html><html lang="fr"><head><meta charset="utf-8">
            <title>TasksWidget</title>
            <style>
              body { font-family: Segoe UI, system-ui, sans-serif; background: #161616;
                     color: #f5f5f5; display: grid; place-items: center; height: 100vh; margin: 0 }
              div { text-align: center }
              h1 { font-size: 20px; font-weight: 600; margin: 0 0 8px }
              p { color: #b0b0b0; font-size: 14px; margin: 0 }
            </style></head>
            <body><div><h1>$title</h1><p>$message</p></div></body></html>
        """.trimIndent()
        val bytes = body.toByteArray(Charsets.UTF_8)
        output.write(
            buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${bytes.size}\r\n")
                append("Connection: close\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)
        )
        output.write(bytes)
        output.flush()
    }

    private fun exchangeCode(
        credentials: OAuthCredentials,
        code: String,
        verifier: String,
        redirectUri: String
    ) {
        val form = buildString {
            append("code=").append(encode(code))
            append("&client_id=").append(encode(credentials.clientId))
            // Google réclame ce champ même pour une application installée, où il
            // ne protège rien : PKCE est ce qui lie réellement le code à cette
            // demande. Il reste hors du dépôt pour ne pas alerter les scanners.
            if (credentials.clientSecret.isNotBlank())
                append("&client_secret=").append(encode(credentials.clientSecret))
            append("&code_verifier=").append(encode(verifier))
            append("&redirect_uri=").append(encode(redirectUri))
            append("&grant_type=authorization_code")
        }
        val tokens = postForm(form)
        if (tokens == null) {
            state = State.Failed("Échange du code refusé")
            return
        }
        tokens.get("refresh_token")?.asString?.let { SecretStore.save(it) }
        applyTokens(tokens)
    }

    /** Renouvelle le jeton d'accès ; renvoie `null` si le compte n'est plus valide. */
    private fun refresh(refreshToken: String): String? {
        val credentials = OAuthCredentials.current ?: return null
        val form = buildString {
            append("refresh_token=").append(encode(refreshToken))
            append("&client_id=").append(encode(credentials.clientId))
            if (credentials.clientSecret.isNotBlank())
                append("&client_secret=").append(encode(credentials.clientSecret))
            append("&grant_type=refresh_token")
        }
        val tokens = postForm(form) ?: return null
        applyTokens(tokens)
        return tokens.get("access_token")?.asString
    }

    private fun postForm(form: String): JsonObject? = runCatching {
        val response = http.send(
            HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() !in 200..299) {
            log("GoogleAuth: jeton refusé (HTTP ${response.statusCode()}) ${response.body()}")
            return null
        }
        gson.fromJson(response.body(), JsonObject::class.java)
    }.onFailure { log("GoogleAuth: appel du jeton impossible\n" + it.stackTraceToString()) }
        .getOrNull()

    /** Retient le jeton d'accès et publie l'identité lue dans l'`id_token`. */
    private fun applyTokens(tokens: JsonObject) {
        accessToken = tokens.get("access_token")?.asString
        val expiresIn = tokens.get("expires_in")?.asLong ?: 3600L
        accessTokenExpiry = System.currentTimeMillis() + expiresIn * 1000

        val identity = tokens.get("id_token")?.asString?.let(::readIdentity)
        state = when {
            identity != null -> identity
            state is State.SignedIn -> state // un rafraîchissement ne renvoie pas toujours d'id_token
            accessToken != null -> State.SignedIn("", "", "")
            else -> State.SignedOut
        }
    }

    /**
     * Lit le compte dans la charge utile de l'`id_token`. La signature n'est pas
     * vérifiée : le jeton vient d'arriver par TLS directement de Google, il ne
     * sert qu'à afficher un nom, et rien ici n'accorde de droits sur sa foi.
     */
    private fun readIdentity(idToken: String): State.SignedIn? = runCatching {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        val json = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        val claims = gson.fromJson(json, JsonObject::class.java)
        State.SignedIn(
            email = claims.get("email")?.asString.orEmpty(),
            name = claims.get("name")?.asString.orEmpty(),
            picture = claims.get("picture")?.asString.orEmpty()
        )
    }.getOrNull()

    /**
     * Ouvre l'URL d'autorisation dans le navigateur par défaut.
     *
     * `Desktop.browse` est essayé d'abord mais ne suffit pas : depuis un fil de
     * l'application Compose, il rend la main sans rien ouvrir ni lever la
     * moindre erreur — la connexion restait alors en attente d'un navigateur qui
     * ne venait jamais. On se rabat sur `rundll32 url.dll,FileProtocolHandler`,
     * qui reçoit l'URL comme argument unique : `cmd /c start`, lui, couperait
     * l'adresse au premier `&`, dont une URL OAuth est truffée.
     */
    /** Ce que Windows expose pour « ouvrir » une adresse comme le ferait un clic. */
    private interface Shell32 : com.sun.jna.Library {
        fun ShellExecuteW(
            hwnd: com.sun.jna.Pointer?,
            operation: com.sun.jna.WString?,
            file: com.sun.jna.WString,
            parameters: com.sun.jna.WString?,
            directory: com.sun.jna.WString?,
            showCmd: Int
        ): com.sun.jna.Pointer
    }

    private val shell32: Shell32? by lazy {
        runCatching { com.sun.jna.Native.load("shell32", Shell32::class.java) }.getOrNull()
    }

    /**
     * Ouvre l'URL d'autorisation dans le navigateur par défaut.
     *
     * Deux fausses pistes avant celle-ci : `Desktop.browse` rend la main sans
     * rien ouvrir ni lever d'erreur depuis un fil de l'application, et
     * `rundll32 url.dll,FileProtocolHandler` ouvre un onglet **vide** — il
     * digère mal les `%` dont une URL OAuth encodée est pleine. `ShellExecute`
     * est ce que Windows appelle lui-même quand on clique sur un lien : elle
     * reçoit l'adresse telle quelle et rend un code d'erreur exploitable.
     */
    private fun openBrowser(url: String) {
        val shell = shell32
        if (shell != null) {
            // Au-delà de 32, ShellExecute a réussi ; en deçà, c'est un code
            // d'erreur hérité de l'API 16 bits.
            val result = runCatching {
                com.sun.jna.Pointer.nativeValue(
                    shell.ShellExecuteW(
                        null,
                        com.sun.jna.WString("open"),
                        com.sun.jna.WString(url),
                        null,
                        null,
                        1 // SW_SHOWNORMAL
                    )
                )
            }.getOrDefault(0L)
            if (result > 32L) return
            log("GoogleAuth: ShellExecute a refusé l'URL (code $result)")
        }

        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(URI.create(url))
            else throw IllegalStateException("aucun moyen d'ouvrir un navigateur")
        }.onFailure {
            log("GoogleAuth: navigateur impossible à ouvrir\n" + it.stackTraceToString())
            state = State.Failed("Navigateur impossible à ouvrir")
        }
    }

    private fun randomUrlSafe(bytes: Int): String {
        val buffer = ByteArray(bytes)
        SecureRandom().nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    private fun sha256Base64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII)))

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decode(value: String): String =
        runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
