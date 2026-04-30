// network/models/LoginRequest.kt
package com.alsmsrecive.dev.network.models

data class LoginRequest(
    val identifier: String,
    val password: String,
    val deviceId: String,
    val deviceName: String
)