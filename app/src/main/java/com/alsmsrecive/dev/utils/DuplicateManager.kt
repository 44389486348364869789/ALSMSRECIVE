package com.alsmsrecive.dev.utils

import android.content.Context
import java.io.File
import java.security.MessageDigest

object DuplicateManager {

    private const val FILE_NAME = "message_history_v3.txt"
    private const val MAX_HISTORY = 100 // শেষ ১০০টি মেসেজ মনে রাখবে

    private fun hashString(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input // Fallback
        }
    }

    fun isDuplicate(context: Context, uniqueKey: String): Boolean {
        val hashedKey = hashString(uniqueKey)
        val file = File(context.getExternalFilesDir(null), FILE_NAME)

        // ১. ফাইল না থাকলে তৈরি করি
        if (!file.exists()) {
            file.createNewFile()
        }

        // ২. ফাইলের সব ডাটা রিড করি (লাইন বাই লাইন)
        val historyList = file.readLines().toMutableList()

        // ৩. চেক করি এই মেসেজটি কি ইতিমধ্যে লিস্টে আছে?
        if (historyList.contains(hashedKey)) {
            return true // ডুপ্লিকেট পাওয়া গেছে
        }

        // ৪. নতুন মেসেজ লিস্টে যোগ করি
        historyList.add(0, hashedKey) // সবার উপরে যোগ করি

        // ৫. যদি ১০০ এর বেশি হয়, নিচ থেকে ডিলিট করে দিই
        if (historyList.size > MAX_HISTORY) {
            historyList.subList(MAX_HISTORY, historyList.size).clear()
        }

        // ৬. ফাইলে আবার সেভ করি
        file.writeText(historyList.joinToString("\n"))

        return false // এটি নতুন মেসেজ
    }
}