// network/models/MessageRequest.kt
package com.example.alsmsrecive.network.models

data class MessageRequest(
    val type: String,
    val sender: String,
    val message: String
)