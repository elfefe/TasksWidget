package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class OtherPlatformOauthClient (
    @SerializedName("client_id")
    @Expose
    var clientId: String,

    @SerializedName("client_type")
    @Expose
    var clientType: Int = 0
)