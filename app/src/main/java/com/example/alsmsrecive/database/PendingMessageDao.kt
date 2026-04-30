// database/PendingMessageDao.kt (নতুন ফাইল)
package com.example.alsmsrecive.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingMessageDao {
    // সেভ করা সব মেসেজ লোড করা
    @Query("SELECT * FROM pending_messages")
    suspend fun getAllPendingMessages(): List<PendingMessage>

    // নতুন মেসেজ সেভ করা
    @Insert
    suspend fun insertMessage(message: PendingMessage)

    // সার্ভারে পাঠানো হয়ে গেলে মেসেজটি ডিলিট করা
    @Delete
    suspend fun deleteMessage(message: PendingMessage)
}