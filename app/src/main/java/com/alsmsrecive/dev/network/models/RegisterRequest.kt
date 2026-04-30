// network/models/RegisterRequest.kt
package com.alsmsrecive.dev.network.models

data class RegisterRequest(
    val email: String?,
    val phone: String?,
    val password: String
)