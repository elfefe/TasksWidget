package com.elfefe.common.model

data class User(val email: String, val name: String, val picture: String, val contexts: MutableList<Context>)
