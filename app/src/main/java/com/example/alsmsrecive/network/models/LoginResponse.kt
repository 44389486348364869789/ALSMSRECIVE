// network/models/LoginResponse.kt
package com.example.alsmsrecive.network.models

data class LoginResponse(
    val token: String,
    val planExpiresAt: String?
)