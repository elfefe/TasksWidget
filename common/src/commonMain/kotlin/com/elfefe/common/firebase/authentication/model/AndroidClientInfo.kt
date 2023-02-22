package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AndroidClientInfo (
    @SerializedName("package_name")
    @Expose
    var packageName: String
)