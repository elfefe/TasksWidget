package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ClientInfo (
    @SerializedName("mobilesdk_app_id")
    @Expose
    var mobilesdkAppId: String,
    @SerializedName("android_client_info")
    @Expose
    var androidClientInfo: AndroidClientInfo
)