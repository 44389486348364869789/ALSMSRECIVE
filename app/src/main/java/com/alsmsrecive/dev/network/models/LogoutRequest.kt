package com.alsmsrecive.dev.network.models

data class LogoutRequest(
    val email: String,
    val deviceId: String
)