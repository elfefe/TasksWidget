package com.elfefe.common.controller.auth

import androidx.compose.ui.res.useResource
import com.elfefe.common.controller.appPrivateDir
import com.elfefe.common.controller.log
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

/**
 * Identifiants du client OAuth, **tenus hors du dépôt**.
 *
 * Google exige un `client_secret` même pour une application installée, tout en
 * reconnaissant qu'il n'y protège rien : il finit forcément dans le binaire
 * distribué, PKCE étant ce qui lie réellement un code d'autorisation à la
 * demande qui l'a produit. Il n'a pas pour autant sa place dans un dépôt public
 * — ce projet a déjà connu une purge de secrets, et un scanner le signalerait à
 * juste titre.
 *
 * Ils sont donc lus, dans l'ordre :
 * 1. la ressource `oauth-client.json`, déposée à la construction depuis les
 *    secrets du dépôt ;
 * 2. un fichier `oauth-client.json` à côté de la configuration, pour essayer
 *    sans reconstruire ;
 * 3. les variables d'environnement `TW_OAUTH_CLIENT_ID` / `TW_OAUTH_CLIENT_SECRET`.
 *
 * Sans aucune des trois, la connexion est simplement indisponible et le reste de
 * l'application fonctionne comme avant.
 */
class OAuthCredentials(val clientId: String, val clientSecret: String) {

    companion object {
        private const val RESOURCE = "oauth-client.json"

        val current: OAuthCredentials? by lazy {
            fromResource() ?: fromFile() ?: fromEnvironment()
        }

        private fun fromResource(): OAuthCredentials? = runCatching {
            useResource(RESOURCE) { parse(it.readBytes().toString(Charsets.UTF_8)) }
        }.getOrNull()

        private fun fromFile(): OAuthCredentials? = runCatching {
            val file = File(appPrivateDir, RESOURCE)
            if (!file.exists()) null else parse(file.readText())
        }.onFailure { log("OAuthCredentials: fichier illisible\n" + it.stackTraceToString()) }
            .getOrNull()

        private fun fromEnvironment(): OAuthCredentials? {
            val id = System.getenv("TW_OAUTH_CLIENT_ID")?.trim().orEmpty()
            val secret = System.getenv("TW_OAUTH_CLIENT_SECRET")?.trim().orEmpty()
            return if (id.isBlank()) null else OAuthCredentials(id, secret)
        }

        /**
         * Accepte aussi bien un objet plat que le fichier téléchargé tel quel
         * depuis la console Google, qui range tout sous une clé `installed`.
         */
        private fun parse(content: String): OAuthCredentials? {
            val root = Gson().fromJson(content, JsonObject::class.java) ?: return null
            val holder = root.getAsJsonObject("installed")
                ?: root.getAsJsonObject("web")
                ?: root
            val id = holder.get("client_id")?.asString?.trim().orEmpty()
            val secret = holder.get("client_secret")?.asString?.trim().orEmpty()
            return if (id.isBlank()) null else OAuthCredentials(id, secret)
        }
    }
}
