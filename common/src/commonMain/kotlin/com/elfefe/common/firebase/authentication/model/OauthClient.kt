package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class OauthClient (
    @SerializedName("client_id")
    @Expose
    var clientId: String,

    @SerializedName("client_type")
    @Expose
    var clientType: Int = 0,

    @SerializedName("android_info")
    @Expose
    var androidInfo: AndroidInfo
)