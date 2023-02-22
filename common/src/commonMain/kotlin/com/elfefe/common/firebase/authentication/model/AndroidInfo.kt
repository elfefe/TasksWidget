package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AndroidInfo (
    @SerializedName("package_name")
    @Expose
    var packageName: String,

    @SerializedName("certificate_hash")
    @Expose
    var certificateHash: String
)