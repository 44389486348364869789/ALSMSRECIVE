// MainApplication.kt (নতুন ফাইল)
package com.alsmsrecive.dev

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class MainApplication : Application(), Configuration.Provider {

    // ডেটাবেজ ইন্সট্যান্স (ঐচ্ছিক, তবে ভালো অভ্যাস)
    val database: com.alsmsrecive.dev.database.AppDatabase by lazy {
        com.alsmsrecive.dev.database.AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        // অ্যাপ চালু হলেই ডেটাবেজ ইন্সট্যান্স তৈরি হবে
        database
        
        // Fix: Set Device ID so background workers (SyncMessageWorker) send x-device-id
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        com.alsmsrecive.dev.network.ApiClient.setDeviceId(deviceId)
    }

    // WorkManager-কে ডিফল্ট কনফিগারেশন দেওয়ার জন্য
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}