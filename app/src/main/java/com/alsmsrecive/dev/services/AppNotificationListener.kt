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
            val lastMessage = messages.last() as? android.os.Bundle
            if (lastMessage != null) {
                val msgText = lastMessage.getCharSequence("text")?.toString()
                if (!msgText.isNullOrEmpty()) {
                    extractedText = msgText
                    
                    // API 28+ uses 'sender_person', older uses 'sender'
                    var senderName: String? = null
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val senderPerson = lastMessage.getParcelable<android.app.Person>("sender_person")
                        senderName = senderPerson?.name?.toString()
                    }
                    if (senderName.isNullOrEmpty()) {
                        senderName = lastMessage.getCharSequence("sender")?.toString()
                    }
                    
                    if (!senderName.isNullOrEmpty()) {
                        extractedTitle = senderName // If group chat, use sender's name
                    }
                }
            }
        } 
        
        // b) InboxStyle (Older Apps or Email)
        if (extractedText.isEmpty()) {
            val textLines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
            if (textLines != null && textLines.isNotEmpty()) {
                extractedText = textLines.last().toString() // Get the last line only
            }
        }
        
        // c) Fallback (Standard Text/BigText)
        if (extractedText.isEmpty()) {
            extractedText = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: return
        }

        if (extractedText.isEmpty()) return

        // ৩. লুপ আটকানো (নিজের বট ইগনোর)
        // যদি টেক্সটের শেষে "(Synced)" থাকে, তার মানে এটা আমাদেরই পাঠানো মেসেজ
        if (extractedText.contains("(Synced)")) {
            Log.d("NotificationListener", "Ignoring own Bot message: $extractedTitle")
            return
        }
        if (packageName == applicationContext.packageName) {
            return
        }

        // --- !!! ফাইল বেসড সিকিউর ডুপ্লিকেট চেকিং (SHA-256) !!! ---

        // ইউনিক চাবি তৈরি: অ্যাপ + টাইটেল + মেসেজ
        val uniqueKey = "$packageName|$extractedTitle|$extractedText"

        // চেক করুন এটি আগে পাঠানো হয়েছে কিনা
        if (DuplicateManager.isDuplicate(applicationContext, uniqueKey)) {
            Log.d("NotificationListener", "Duplicate Blocked (Hash History): $extractedTitle")
            return // সার্ভারে পাঠাবো না
        }
        // -----------------------------------------------------

        // অ্যাপের নাম
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        Log.d("NotificationListener", "Sending NEW: $appName - $extractedTitle")

        // সার্ভারে পাঠানো
        MessageRepository.sendMessageToServer(
            applicationContext,
            appName,
            extractedTitle,
            extractedText
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // দরকার নেই
    }
}