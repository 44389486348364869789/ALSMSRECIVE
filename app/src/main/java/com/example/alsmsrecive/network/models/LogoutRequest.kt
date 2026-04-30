package com.example.alsmsrecive.network.models

data class LogoutRequest(
    val email: String,
    val deviceId: String
)