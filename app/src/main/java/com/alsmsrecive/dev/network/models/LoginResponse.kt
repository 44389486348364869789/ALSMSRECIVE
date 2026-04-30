// network/models/LoginResponse.kt
package com.alsmsrecive.dev.network.models

data class LoginResponse(
    val token: String,
    val planExpiresAt: String?
)