// network/models/RegisterRequest.kt
package com.example.alsmsrecive.network.models

data class RegisterRequest(
    val email: String?,
    val phone: String?,
    val password: String
)