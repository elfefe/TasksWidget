package com.elfefe.common.firebase.authentication

import com.google.gson.Gson
import com.jcraft.jsch.jce.SHA256
import com.elfefe.common.firebase.authentication.model.JWToken
import com.elfefe.common.firebase.authentication.model.Payload
import io.jsonwebtoken.Jwts.header
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*


private val HTML_HEADER =
    """
        <html>
            <head>
                <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Sofia&effect=fire">
                <meta http-equiv='refresh' content='10;url=https://google.com'>
            </head>
            <style>
            body {
              font-family: "Sofia", sans-serif;
              font-size: 30px;
            }
            </style>
            <body>
                <h3 class="font-effect-fire">You're successfully connected !</h3>
            </body>
        </html>""".trimIndent()


class OAuthApi(private val scope: CoroutineScope) {

    fun auth(clientId: String, clientSecret: String, onConnected: (JWToken, Payload) -> Unit) {
        val state = generateCodeVerifier()
        val codeVerifier = generateCodeVerifier()

        val codeChallenge = generateCodeChallenge(codeVerifier)

        val client = HttpClient()

        scope.launch(Dispatchers.IO) {
            var asSent = false
            embeddedServer(
                Netty,
                port = 8080,
                host = "127.0.0.1",
                module = {
                    routing {
                        get("/") {
                            call.respondText(HTML_HEADER, ContentType.Text.Html)
                            val code = call.parameters["code"]

                            if (!asSent) {
                                asSent = true
                                val response = client.post(TOKEN_ENDPOINT) {
                                    contentType(ContentType.Application.FormUrlEncoded)
                                    accept(ContentType.Text.Html)
                                    accept(ContentType("application", "xhtml+xml"))
                                    accept(ContentType("application", "xml;q=0.9,*/*;q=0.8"))
                                    setBody("code=$code&redirect_uri=$REDIRECT_URI&client_id=$clientId&code_verifier=$codeVerifier&client_secret=$clientSecret&scope=https://www.googleapis.com/auth/cloud-platform&grant_type=authorization_code")
                                }

                                val jwToken = Gson().fromJson(response.string, JWToken::class.java)

                                val tokenSplit = jwToken.idToken.split(".")

                                val user = Base64.getDecoder().decode(tokenSplit[1]).decodeToString()

                                val payload = Gson().fromJson(user, Payload::class.java)

                                scope.launch {
                                    onConnected(jwToken, payload)
                                }
                            }
                        }
                    }
                })
                .start(wait = true)
        }


        Desktop.getDesktop().browse(buildAuthUrl(clientId, state, codeChallenge))
    }

    fun get(url: String, headers: Map<String, String>, response: CoroutineScope.(String) -> Unit) =
        scope.launch(Dispatchers.IO) {
            response(
                HttpClient().get(url) {
                    header(headers)
                }.string
            )
        }


    private fun buildPayload(content: String): Payload {

        val jwToken = Gson().fromJson(content, JWToken::class.java)

        val tokenSplit = jwToken.idToken.split(".")

        val user = Base64.getDecoder().decode(tokenSplit[1]).decodeToString()

        return Gson().fromJson(user, Payload::class.java)
    }

    private fun buildAuthUrl(
        clientId: String,
        state: String,
        codeChallenge: String
    ) =
        URI("$AUTHORIZATION_ENDPOINT?response_type=code&scope=openid%20profile%20email%20https://www.googleapis.com/auth/cloud-platform%20https://www.googleapis.com/auth/datastore&redirect_uri=$REDIRECT_URI&client_id=$clientId&state=$state&code_challenge=$codeChallenge&code_challenge_method=$CODE_CHALLENGE_METHOD")

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
    }

    private fun generateCodeChallenge(codeVerifier: String): String =
        SHA256().run {
            val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
            init()
            update(bytes, 0, bytes.size)
            Base64.getUrlEncoder().withoutPadding().encodeToString(digest())
        }

    companion object {
        private val AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private val TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token"
        private val USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
        private val CLIENT_ID = ""
        private val CLIENT_SECRET = ""
        private val SCOPE = "openid profile"
        private val REDIRECT_URI = "http://localhost:8080/"
        private val CODE_CHALLENGE_METHOD = "S256"
        private val GRANT_TYPE = "authorization_code"
        private val RESPONSE_TYPE = "code"
        private val ACCEPT = "Accept=text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        private val CONTENT_TYPE = "application/x-www-form-urlencoded"
        val AUTHORIZATION = "Authorization"
        private val BEARER = "Bearer "
        private val UTF_8 = "UTF-8"
        private val ACCESS_TOKEN = "access_token"
        private val ERROR = "error"
        private val CODE = "code"
        private val STATE_PARAM = "state"
        private val CODE_VERIFIER_PARAM = "code_verifier"
        private val CODE_CHALLENGE_PARAM = "code_challenge"
        private val CODE_CHALLENGE_METHOD_PARAM = "code_challenge_method"
        private val GRANT_TYPE_PARAM = "grant_type"
        private val RESPONSE_TYPE_PARAM = "response_type"
        private val REDIRECT_URI_PARAM = "redirect_uri"
        private val CLIENT_ID_PARAM = "client_id"
        private val SCOPE_PARAM = "scope"
        private val STATE_PARAM_PARAM = "state"
        private val CLIENT_SECRET_PARAM = "client_secret"
        private val CODE_PARAM = "code"
        private val QUESTION_MARK = "?"
        private val AMPERSAND = "&"
        private val EQUALS = "="
        private val EMPTY_STRING = ""
    }
}

@OptIn(InternalAPI::class)
private val io.ktor.client.statement.HttpResponse.string: String
    get() = content.toInputStream().readBytes().decodeToString()
