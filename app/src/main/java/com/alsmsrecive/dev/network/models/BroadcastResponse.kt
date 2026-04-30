package com.alsmsrecive.dev.network.models

data class BroadcastResponse(
    val _id: String,
    val title: String,
    val message: String,
    val imageUrl: String?,
    val link: String?,
    val linkText: String?,
    val isActive: Boolean
)
