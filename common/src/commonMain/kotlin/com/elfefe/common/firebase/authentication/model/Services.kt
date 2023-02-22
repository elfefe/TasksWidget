package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Services (
    @SerializedName("appinvite_service")
    @Expose
    var appinviteService: AppinviteService
)