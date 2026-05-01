package com.alsmsrecive.dev.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.alsmsrecive.dev.repository.MessageRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""

                if (messageBody.isEmpty()) continue

                // --- STRICT DUPLICATE CHECK (Hash-Based) ---
                val uniqueSmsKey = "SMS|$sender|$messageBody"

                if (com.alsmsrecive.dev.utils.DuplicateManager.isDuplicate(context.applicationContext, uniqueSmsKey)) {
                    Log.d("SmsReceiver", "Duplicate SMS Blocked by Hash")
                    continue // Skip this message
                }

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