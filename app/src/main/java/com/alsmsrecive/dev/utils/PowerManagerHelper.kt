package com.alsmsrecive.dev.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

object PowerManagerHelper {

    // isManual = false (লগইনের সময়), isManual = true (মেনু বাটনে ক্লিক করলে)
    fun requestBackgroundPermission(context: Context, isManual: Boolean) {

        // ১. প্রথমে স্ট্যান্ডার্ড অ্যান্ড্রয়েড চেক (Samsung/Pixel এর জন্য ভালো)
        if (!isIgnoringBatteryOptimizations(context)) {
            requestGenericBatteryOptimization(context)
            return
        }

        // ২. যদি পারমিশন থাকেও, বা ইউজার মেনু থেকে ক্লিক করে (Realme/Xiaomi ফিক্স)
        // তখন আমরা কোনো রিস্ক নেবো না, সরাসরি "App Info" পেজে নিয়ে যাবো।
        if (isManual) {
            openAppInfoPage(context)
        }
    }

    // অ্যাপ ইনফো পেজ ওপেন করার ফাংশন (সব ফোনে কাজ করবে)
    private fun openAppInfoPage(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            // ইউজারকে বলে দেওয়া কী করতে হবে
            Toast.makeText(context, "Click 'Battery usage' > Turn ON 'Allow background activity' ⚡", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pw = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pw.isIgnoringBatteryOptimizations(name)
        } else {
            true
        }
    }

    private fun requestGenericBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                // যদি ডাইরেক্ট পপ-আপ না আসে, তবে অ্যাপ ইনফোতে নিয়ে যাও
                openAppInfoPage(context)
            }
        }
    }
}