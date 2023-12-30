package com.elfefe.common.controller.firebase.authentication.model

import com.google.gson.annotations.SerializedName

data class JWToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    val scope: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("id_token")
    val idToken: String,
)
