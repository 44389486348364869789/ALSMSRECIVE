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

        // --- স্মার্ট নোটিফিকেশন পার্সিং (টেলিগ্রাম/হোয়াটসঅ্যাপের জন্য) ---
        
        // a) MessagingStyle (Modern Apps like Telegram, WhatsApp)
        val messages = extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            for (msgObj in messages) {
                val msgBundle = msgObj as? android.os.Bundle ?: continue
                val msgText = msgBundle.getCharSequence("text")?.toString() ?: continue
                if (msgText.isEmpty()) continue

                // API 28+ uses 'sender_person', older uses 'sender'
                var senderName: String? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val senderPerson = msgBundle.getParcelable<android.app.Person>("sender_person")
                    senderName = senderPerson?.name?.toString()
                }
                if (senderName.isNullOrEmpty()) {
                    senderName = msgBundle.getCharSequence("sender")?.toString()
                }
                
                val finalTitle = if (!senderName.isNullOrEmpty()) senderName else extractedTitle
                val msgTime = msgBundle.getLong("time", sbn.notification.`when`)

                processAndSend(packageName, finalTitle, msgText, msgTime)
            }
            return // Processed all messages in bundle, no need to fallback
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
            processAndSend(packageName, extractedTitle, fallbackText, sbn.notification.`when`)
        }
    }

    private fun processAndSend(packageName: String, title: String, text: String, msgTime: Long) {
        // ৩. লুপ আটকানো (নিজের বট ইগনোর)
        if (text.contains("(Synced)")) {
            Log.d("NotificationListener", "Ignoring own Bot message: $title")
            return
        }
        if (packageName == applicationContext.packageName) {
            return
        }

        // --- !!! ফাইল বেসড সিকিউর ডুপ্লিকেট চেকিং (SHA-256) !!! ---
        // আমরা title ব্যবহার করছি না কারণ অনেক সময় "User (2 messages)" হয়ে title চেঞ্জ হয়ে যায়!
        val uniqueKey = "$packageName|$text|$msgTime"

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