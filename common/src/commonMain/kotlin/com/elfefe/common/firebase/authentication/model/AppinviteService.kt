package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AppinviteService (
    @SerializedName("other_platform_oauth_client")
    @Expose
    var otherPlatformOauthClient: List<OtherPlatformOauthClient>
)