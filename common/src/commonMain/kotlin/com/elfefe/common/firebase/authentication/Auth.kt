package com.elfefe.common.firebase.authentication

import androidx.compose.ui.window.application
import com.elfefe.common.firebase.authentication.model.GoogleService
import com.google.gson.Gson
import java.io.File



class Auth {
    val apiKey by lazy { Gson().fromJson(File("google-services.json").readText(), GoogleService::class.java) }

    init {
        println(File("").list().toList())
        println(apiKey.client[0].apiKey)
    }
}