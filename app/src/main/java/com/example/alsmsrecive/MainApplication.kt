// MainApplication.kt (নতুন ফাইল)
package com.example.alsmsrecive

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class MainApplication : Application(), Configuration.Provider {

    // ডেটাবেজ ইন্সট্যান্স (ঐচ্ছিক, তবে ভালো অভ্যাস)
    val database: com.example.alsmsrecive.database.AppDatabase by lazy {
        com.example.alsmsrecive.database.AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        // অ্যাপ চালু হলেই ডেটাবেজ ইন্সট্যান্স তৈরি হবে
        database
    }

    // WorkManager-কে ডিফল্ট কনফিগারেশন দেওয়ার জন্য
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}