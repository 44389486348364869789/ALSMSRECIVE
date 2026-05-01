// repository/MessageRepository.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alsmsrecive.dev.database.AppDatabase
import com.alsmsrecive.dev.database.PendingMessage
import com.alsmsrecive.dev.network.ApiClient
// import com.alsmsrecive.dev.network.TelegramApiClient // <-- এটি আর এখানে দরকার নেই
import com.alsmsrecive.dev.network.models.MessageRequest
import com.alsmsrecive.dev.utils.SessionManager
import com.alsmsrecive.dev.utils.EncryptionUtil
import com.alsmsrecive.dev.workers.SyncMessageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// import java.time.LocalDateTime // <-- এটি আর এখানে দরকার নেই
// import java.time.format.DateTimeFormatter // <-- এটি আর এখানে দরকার নেই
import com.alsmsrecive.dev.network.CallLog as ApiCallLog

object MessageRepository {

    const val ACTION_NEW_MESSAGE_RECEIVED = "ACTION_NEW_MESSAGE_RECEIVED"

    // --- sendMessageToServer (সম্পূর্ণ নতুন লজিক) ---
    fun sendMessageToServer(
        context: Context,
        type: String,
        sender: String,
        message: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val sessionManager = SessionManager(context.applicationContext)
            val token = sessionManager.getAuthToken()

            // যদি ইউজার লগইন না থাকে, তবে কিছুই করবেন না
            if (token == null) {
                Log.e("MessageRepo", "User not logged in. Message discarded.")
                return@launch
            }

            // ধাপ ১: মেসেজটি লোকাল ডেটাবেজে সেভ করুন (এনক্রিপ্ট করে)
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                
                val password = sessionManager.getUserPassword()
                val encryptedMessage = EncryptionUtil.encrypt(message, password)
                val encryptedSender = EncryptionUtil.encrypt(sender, password)

                val pendingMessage = PendingMessage(
                    type = type,
                    sender = encryptedSender,
                    message = encryptedMessage,
                    token = token,
                    deviceId = sessionManager.getDeviceId(),
                    deviceName = sessionManager.getDeviceName()
                )
                db.pendingMessageDao().insertMessage(pendingMessage)
                Log.d("MessageRepo", "Message saved locally to Room database.")

                // MainActivity-কে জানানোর জন্য ব্রডকাস্ট (আগের মতোই)
                // এটি এখন সাথে সাথে লিস্ট রিফ্রেশ না করলেও চলবে, কারণ মেসেজ সার্ভারে যায়নি
                // তবে WorkManager সফল হলে ব্রডকাস্ট পাঠানো যেতে পারে
                // আপাতত এটি রাখছি
                val intent = Intent(ACTION_NEW_MESSAGE_RECEIVED)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

            } catch (e: Exception) {
                Log.e("MessageRepo", "Failed to save message to Room DB: ${e.message}")
            }

            // ধাপ ২: WorkManager-কে চালু করুন
            triggerMessageSyncWorker(context.applicationContext)
        }
    }

    // --- !!! নতুন ফাংশন: WorkManager চালু করা !!! ---
    fun triggerMessageSyncWorker(context: Context) {
        // শর্ত: ডিভাইসটি ইন্টারনেটের সাথে সংযুক্ত থাকতে হবে
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // একটি ওয়ান-টাইম রিকোয়েস্ট তৈরি করুন
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncMessageWorker>()
            .setConstraints(constraints)
            .build()

        // রিকোয়েস্টটি WorkManager-এর কাছে জমা দিন
        WorkManager.getInstance(context).enqueue(syncWorkRequest)
        Log.d("MessageRepo", "SyncMessageWorker enqueued.")
    }
    // ------------------------------------------


    // --- CallLog ফাংশন (অপরিবর্তিত) ---
    // (syncCallLogs এবং অন্যান্য হেল্পার ফাংশন আগের মতোই থাকবে)
    private fun android.database.Cursor.whileMoveToNext(block: android.database.Cursor.() -> Unit) {
        while (moveToNext()) {
            block()
        }
    }

    @SuppressLint("Range")
    fun syncCallLogs(context: Context, token: String, onComplete: (Boolean, String) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            onComplete(false, "Call Log permission not granted")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callLogList = mutableListOf<ApiCallLog>()
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    null, null, null,
                    CallLog.Calls.DATE + " DESC"
                )

                if (cursor != null && cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                        val typeInt = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                        val date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
                        val duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION))

                        val type = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                            else -> "UNKNOWN"
                        }

                        val sessionManager = com.alsmsrecive.dev.utils.SessionManager(context)
                        val password = sessionManager.getUserPassword() ?: "default_pass"
                        val encryptedNumber = com.alsmsrecive.dev.utils.EncryptionUtil.encrypt(number ?: "Unknown", password)

                        callLogList.add(ApiCallLog(encryptedNumber, type, date, duration))
                    }
                }
                cursor?.close()

                val response = ApiClient.instance.syncCallLogs(token, callLogList)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) { onComplete(true, "Synced ${callLogList.size} call logs.") }
                } else {
                    withContext(Dispatchers.Main) { onComplete(false, "Failed to sync call logs: ${response.message()}") }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(false, "Network Error") }
            }
        }
    }
}