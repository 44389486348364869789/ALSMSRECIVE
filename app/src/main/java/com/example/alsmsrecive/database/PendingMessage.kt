// database/PendingMessage.kt (নতুন ফাইল)
package com.example.alsmsrecive.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val sender: String,
    val message: String,
    val token: String // মেসেজটি কোন ইউজারের তা পাঠানোর জন্য টোকেন সেভ করা জরুরি
)