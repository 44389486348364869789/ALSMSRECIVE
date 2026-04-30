// network/models/LoginResponse.kt
package com.alsmsrecive.dev.network.models

data class LoginResponse(
    val token: String,
    val planExpiresAt: String?,
    val telegramBotToken: String?,
    val telegramChatId: String?
)