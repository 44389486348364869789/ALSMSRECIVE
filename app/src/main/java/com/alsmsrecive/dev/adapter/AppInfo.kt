package com.alsmsrecive.dev.adapter

import android.graphics.drawable.Drawable

// এই ক্লাসটি প্রতিটি অ্যাপের নাম, প্যাকেজ নেম এবং আইকন ধরে রাখবে
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)