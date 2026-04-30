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
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: return

        // ৩. লুপ আটকানো (নিজের বট ইগনোর)
        // যদি টেক্সটের শেষে "(Synced)" থাকে, তার মানে এটা আমাদেরই পাঠানো মেসেজ
        if (text.contains("(Synced)")) {
            Log.d("NotificationListener", "Ignoring own Bot message: $title")
            return
        }
        if (packageName == applicationContext.packageName) {
            return
        }

        // --- !!! ফাইল বেসড ডুপ্লিকেট চেকিং (১০০ মেসেজ) !!! ---

        // ইউনিক চাবি তৈরি: অ্যাপ + টাইটেল + মেসেজ
        // (সময় বা টাইমস্ট্যাম্প এখানে ব্যবহার করবেন না, কারণ টেলিগ্রাম টাইম আপডেট করে দেয়)
        val uniqueKey = "$packageName|$title|$text"

        // চেক করুন এটি আগে পাঠানো হয়েছে কিনা
        if (DuplicateManager.isDuplicate(applicationContext, uniqueKey)) {
            Log.d("NotificationListener", "Duplicate Blocked (File History): $title")
            return // সার্ভারে পাঠাবো না
        }
        // -----------------------------------------------------

        // অ্যাপের নাম
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        Log.d("NotificationListener", "Sending NEW: $appName - $title")

        // সার্ভারে পাঠানো
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