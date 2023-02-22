package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GoogleService (
    @SerializedName("project_info")
    @Expose
    var projectInfo: ProjectInfo,

    @SerializedName("client")
    @Expose
    var client: List<Client>,

    @SerializedName("configuration_version")
    @Expose
    var configurationVersion: String
)