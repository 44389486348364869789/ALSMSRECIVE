package com.alsmsrecive.dev.network.models

data class LogoutRequest(
    val email: String? = null,
    val phone: String? = null,
    val deviceId: String
)