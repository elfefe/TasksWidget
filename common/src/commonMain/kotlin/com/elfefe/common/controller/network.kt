package com.elfefe.common.controller

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*

val client by lazy {
    HttpClient(CIO) {
        // Set up redirects similar to LaxRedirectStrategy
        install(HttpRedirect) {
            checkHttpMethod = false // Ktor follows all redirects, similar to a "lax" redirect strategy
        }
    }
}
