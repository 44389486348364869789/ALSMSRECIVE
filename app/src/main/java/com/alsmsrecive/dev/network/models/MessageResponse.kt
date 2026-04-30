// network/models/MessageResponse.kt
package com.alsmsrecive.dev.network.models

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val type: String?,
    val sender: String?,
    val message: String?,
    val deviceId: String?,
    val deviceName: String?,
    val timestamp: String
)