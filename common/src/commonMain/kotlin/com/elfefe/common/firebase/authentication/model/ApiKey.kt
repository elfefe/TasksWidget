package com.elfefe.common.firebase.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ApiKey (
    @SerializedName("current_key")
    @Expose
    var currentKey: String
)