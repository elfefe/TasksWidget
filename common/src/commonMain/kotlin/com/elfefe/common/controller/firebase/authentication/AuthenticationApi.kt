package com.elfefe.common.controller.firebase.authentication

import com.elfefe.common.controller.client
import com.elfefe.common.controller.firebase.authentication.model.JWToken
import com.google.cloud.firestore.annotation.Exclude
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuthenticationApi {
    var user: User? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    fun login(user: User) {
        this.user = user

        scope.launch {
            client.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=***SECRET-PURGE-2026-07-27***") {
                setBody(Gson().toJson(user))
            }.let { response ->
                println(response.request.url)
                println(response.bodyAsText())
                val auth = Gson().fromJson(response.bodyAsText(), AuthResponse::class.java)
                user.token = JWToken(
                    accessToken = "",
                    auth.expiresIn ?: "",
                    auth.refreshToken ?: "",
                    "",
                    "",
                    auth.idToken ?: ""
                )
            }
        }
    }

    fun register(user: User) {
        this.user = user

        scope.launch {
            client.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=***SECRET-PURGE-2026-07-27***") {
                setBody(Gson().toJson(user))
            }.let { response ->
                println(response.request.url)
                println(response.bodyAsText())
            }
        }
    }
}

data class User(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
    @Exclude
    @Expose
    var token: JWToken? = null
)

data class AuthResponse(
    val kind: String?,
    val localId: String?,
    val email: String?,
    val displayName: String?,
    val idToken: String?,
    val registered: Boolean?,
    val profilePicture: String?,
    val oauthAccessToken: String?,
    val oauthExpireIn: Int?,
    val oauthAuthorizationCode: String?,
    val refreshToken: String?,
    val expiresIn: String?,
    val mfaPendingCredential: String?,
    val mfaInfo: List<MfaEnrollment>?,
    val userNotifications: List<UserNotification>?
)

enum class UserNotification {
    NOTIFICATION_CODE_UNSPECIFIED,
    MISSING_LOWERCASE_CHARACTER,
    MISSING_UPPERCASE_CHARACTER,
    MISSING_NUMERIC_CHARACTER,
    MISSING_NON_ALPHANUMERIC_CHARACTER,
    MINIMUM_PASSWORD_LENGTH,
    MAXIMUM_PASSWORD_LENGTH
}

data class MfaEnrollment(
    val mfaEnrollmentId: String?,
    val displayName: String?,
    val enrolledAt: String?,
    val phoneInfo: String?,
    val totpInfo: TotpInfo?,
    val emailInfo: EmailInfo?,
    val unobfuscatedPhoneInfo: String?
)

data class TotpInfo(
    val value: String?
)

data class EmailInfo(
    val emailAddress: String?
)
