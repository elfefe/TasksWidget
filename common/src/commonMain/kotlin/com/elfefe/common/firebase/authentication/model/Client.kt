package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Client (
    @SerializedName("client_info")
    @Expose
    var clientInfo: ClientInfo,

    @SerializedName("oauth_client")
    @Expose
    var oauthClient: List<OauthClient>,

    @SerializedName("api_key")
    @Expose
    var apiKey: List<ApiKey>,

    @SerializedName("services")
    @Expose
    var services: Services
)