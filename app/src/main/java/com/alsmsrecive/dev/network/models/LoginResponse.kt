// network/models/LoginResponse.kt
package com.alsmsrecive.dev.network.models

data class LoginResponse(
    val token: String,
    val planExpiresAt: String?,
    val deviceLimit: Int?,
    val activeSessionsCount: Int?,
    val telegramBotToken: String?,
    val telegramChatId: String?
)