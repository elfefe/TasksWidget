package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ProjectInfo (
    @SerializedName("project_number")
    @Expose
    var projectNumber: String,

    @SerializedName("project_id")
    @Expose
    var projectId: String,

    @SerializedName("storage_bucket")
    @Expose
    var storageBucket: String
)