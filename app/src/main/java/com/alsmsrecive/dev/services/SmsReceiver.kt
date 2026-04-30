package com.alsmsrecive.dev.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.alsmsrecive.dev.repository.MessageRepository

class SmsReceiver : BroadcastReceiver() {

    // Static variables to track the last processed SMS content
    companion object {
        private var lastSmsContentKey: String = ""
        private var lastSmsTime: Long = 0
        // If same SMS arrives within 4 seconds, ignore it
        private const val DUPLICATE_WINDOW_MS = 4000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""
                val currentTime = System.currentTimeMillis()

                if (messageBody.isEmpty()) continue

                // --- STRICT DUPLICATE CHECK ---

                // Create unique key from Sender + Message Body
                val uniqueSmsKey = "$sender|$messageBody"

                // Check if exact same message came from same sender recently
                if (uniqueSmsKey == lastSmsContentKey && (currentTime - lastSmsTime) < DUPLICATE_WINDOW_MS) {
                    Log.d("SmsReceiver", "Strict Duplicate SMS Ignored")
                    continue // Skip this message
                }

                // Update tracker
                lastSmsContentKey = uniqueSmsKey
                lastSmsTime = currentTime
                // ------------------------------

                Log.d("SmsReceiver", "Processing SMS from: $sender")

                // Send to Server
                MessageRepository.sendMessageToServer(
                    context.applicationContext,
                    "SMS",
                    sender,
                    messageBody
                )
            }
        }
    }
}