// database/PendingMessage.kt (নতুন ফাইল)
package com.alsmsrecive.dev.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val sender: String,
    val message: String,
    val token: String, // মেসেজটি কোন ইউজারের তা পাঠানোর জন্য টোকেন সেভ করা জরুরি
    val deviceId: String = "unknown",
    val deviceName: String = "Unknown Device",
    val timestamp: Long = System.currentTimeMillis()
)