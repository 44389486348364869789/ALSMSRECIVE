package com.alsmsrecive.dev.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.alsmsrecive.dev.repository.MessageRepository
import com.alsmsrecive.dev.utils.DuplicateManager // নতুন ফাইল ইম্পোর্ট
import com.alsmsrecive.dev.utils.SessionManager

class AppNotificationListener : NotificationListenerService() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // ১. অ্যাপ সিলেকশন চেক
        val selectedPackages = sessionManager.getSelectedAppPackages()
        if (selectedPackages.isNullOrEmpty() || !selectedPackages.contains(packageName)) {
            return
        }

        // Cross-app SMS duplication prevention
        // If it's the default SMS app, ignore it because SmsReceiver already handles SMS perfectly!
        if (packageName == android.provider.Telephony.Sms.getDefaultSmsPackage(applicationContext)) {
            return
        }

        // ২. গ্রুপ মেসেজ বা সামারি ইগনোর
        if (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) {
            return
        }

        val extras = sbn.notification.extras
        var extractedTitle = extras.getString("android.title") ?: "Unknown"
        var extractedText = ""

        // --- স্মার্ট নোটিফিকেশন পার্সিং (যেকোনো অ্যাপের জন্য) ---
        
        // a) MessagingStyle (Modern Apps like Telegram, WhatsApp, Messenger)
        val messagesArray = extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
        if (messagesArray != null && messagesArray.isNotEmpty()) {
            val messages = android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray(messagesArray)
            
            for ((index, msg) in messages.withIndex()) {
                val msgText = msg.text?.toString() ?: continue
                if (msgText.isEmpty()) continue

                val senderName = msg.senderPerson?.name?.toString() ?: msg.sender?.toString() ?: extractedTitle
                
                // If the app doesn't provide a timestamp, it defaults to 0. 
                // We use index as a fallback for uniqueness within the bundle if timestamp is missing.
                val msgTime = msg.timestamp
                val timeOrIndex = if (msgTime > 0) msgTime.toString() else "idx_$index"

                val uniqueKey = "$packageName|$msgText|$timeOrIndex"
                processAndSend(packageName, senderName, msgText, uniqueKey)
            }
            return // Processed all messages in bundle
        } 
        
        // b) InboxStyle (Older Apps or Email) - Fallback to last line to avoid spam
        var fallbackText = ""
        val textLines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            fallbackText = textLines.last().toString() // Get the last line only
        }
        
        // c) Fallback (Standard Text/BigText)
        if (fallbackText.isEmpty()) {
            fallbackText = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: return
        }

        if (fallbackText.isNotEmpty()) {
            // For non-bundled messages, just use text and title (no reliable timestamp)
            val uniqueKey = "$packageName|$extractedTitle|$fallbackText"
            processAndSend(packageName, extractedTitle, fallbackText, uniqueKey)
        }
    }

    private fun processAndSend(packageName: String, title: String, text: String, uniqueKey: String) {
        // ৩. লুপ আটকানো (নিজের বট ইগনোর)
        if (text.contains("(Synced)")) {
            Log.d("NotificationListener", "Ignoring own Bot message: $title")
            return
        }
        if (packageName == applicationContext.packageName) {
            return
        }

        // --- !!! ফাইল বেসড সিকিউর ডুপ্লিকেট চেকিং (SHA-256) !!! ---
        if (DuplicateManager.isDuplicate(applicationContext, uniqueKey)) {
            Log.d("NotificationListener", "Duplicate Blocked (Hash History): $title")
            return
        }

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        Log.d("NotificationListener", "Sending NEW: $appName - $title")

        MessageRepository.sendMessageToServer(
            applicationContext,
            appName,
            title,
            text
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // দরকার নেই
    }
}