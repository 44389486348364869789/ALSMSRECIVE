package com.alsmsrecive.dev.network.models

data class TelegramSettingsRequest(
    val telegramBotToken: String,
    val telegramChatId: String
)
