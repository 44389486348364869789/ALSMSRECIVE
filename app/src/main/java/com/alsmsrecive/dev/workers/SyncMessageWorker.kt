// workers/SyncMessageWorker.kt (নতুন ফাইল)
package com.alsmsrecive.dev.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alsmsrecive.dev.database.AppDatabase
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.network.TelegramApiClient
import com.alsmsrecive.dev.network.models.MessageRequest
import com.alsmsrecive.dev.utils.SessionManager
import com.alsmsrecive.dev.utils.EncryptionUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SyncMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val apiService = ApiClient.instance
    private val telegramService = TelegramApiClient.instance
    private val sessionManager = SessionManager(appContext)

    override suspend fun doWork(): Result {
        Log.d("SyncMessageWorker", "WorkManager started: Checking for pending messages...")

        val pendingMessages = db.pendingMessageDao().getAllPendingMessages()
        if (pendingMessages.isEmpty()) {
            Log.d("SyncMessageWorker", "No pending messages found. Work finished.")
            return Result.success()
        }

        Log.d("SyncMessageWorker", "Found ${pendingMessages.size} pending messages. Attempting to sync...")

        var allSucceeded = true

        for (msg in pendingMessages) {
            try {
                // ১. সার্ভারে পাঠানোর চেষ্টা
                val request = MessageRequest(type = msg.type, sender = msg.sender, message = msg.message)
                val response = apiService.postMessage(msg.token, request)

                if (response.isSuccessful) {
                    Log.d("SyncMessageWorker", "Message (ID: ${msg.id}) sent to OUR server successfully.")

                    // ২. টেলিগ্রামে পাঠানোর চেষ্টা (টেলিগ্রামে পড়ার জন্য ডিক্রিপ্ট করে পাঠাতে হবে)
                    val password = sessionManager.getUserPassword()
                    val plainSender = EncryptionUtil.decrypt(msg.sender, password)
                    val plainMessage = EncryptionUtil.decrypt(msg.message, password)
                    sendToTelegram(msg.type, plainSender, plainMessage)

                    // ৩. সফল হলে ডেটাবেজ থেকে ডিলিট
                    db.pendingMessageDao().deleteMessage(msg)
                } else {
                    Log.e("SyncMessageWorker", "Failed to send Message (ID: ${msg.id}). Will retry later.")
                    allSucceeded = false
                }
            } catch (e: Exception) {
                Log.e("SyncMessageWorker", "Network Error for Message (ID: ${msg.id}): ${e.message}. Will retry later.")
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    // এটি MessageRepository থেকে কপি করা হেল্পার ফাংশন
    private suspend fun sendToTelegram(type: String, sender: String, message: String) {
        try {
            val botToken = sessionManager.getTelegramBotToken()
            val chatId = sessionManager.getTelegramChatId()

            if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM yyyy")
                val formattedTimestamp = now.format(formatter)
                val telegramMessage = "*[${type.uppercase()}]*\n" +
                        "*From: $sender*\n\n" +
                        "$message\n\n" +
                        "`$formattedTimestamp` (Synced)" // বোঝানোর জন্য "Synced" শব্দটি যোগ করা হলো

                val fullToken = "bot$botToken"
                val telegramResponse = telegramService.sendMessage(
                    token = fullToken,
                    chatId = chatId,
                    text = telegramMessage,
                    parseMode = "Markdown"
                )

                if (telegramResponse.isSuccessful) {
                    Log.d("SyncMessageWorker", "Message forwarded to Telegram successfully.")
                } else {
                    Log.e("SyncMessageWorker", "Failed to send to Telegram (Worker): ${telegramResponse.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncMessageWorker", "Error sending to Telegram (Worker): ${e.message}")
        }
    }
}