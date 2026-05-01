// network/models/MessageRequest.kt
package com.alsmsrecive.dev.network.models

data class MessageRequest(
    val type: String,
    val sender: String,
    val message: String,
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long? = null
)