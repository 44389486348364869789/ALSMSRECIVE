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
    fun isDuplicate(context: Context, uniqueKey: String): Boolean {
        val hashedKey = hashString(uniqueKey)
        val file = File(context.getExternalFilesDir(null), FILE_NAME)

        if (!file.exists()) {
            file.createNewFile()
        }

        val historyList = file.readLines().toMutableList()

        if (historyList.contains(hashedKey)) {
            return true // Found exact match (Hash includes timestamp now)
        }

        // Add new hash to top
        historyList.add(0, hashedKey)

        if (historyList.size > MAX_HISTORY) {
            historyList.subList(MAX_HISTORY, historyList.size).clear()
        }

        file.writeText(historyList.joinToString("\n"))
        return false
    }
}