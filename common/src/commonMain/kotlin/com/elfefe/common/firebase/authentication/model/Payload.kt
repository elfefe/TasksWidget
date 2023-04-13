package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.SerializedName

data class Payload(
    val iss: String,
    val azp: String,
    val aud: String,
    val sub: String,
    val hd: String,
    val email: String,
    @SerializedName("email_verified")
    val emailVerified: Boolean,
    @SerializedName("at_hash")
    val atHash: String,
    val name: String,
    val picture: String,
    @SerializedName("given_name")
    val givenName: String,
    @SerializedName("family_name")
    val familyName: String,
    val locale: String,
    val iat: Long,
    val exp: Long
)
