// utils/SessionManager.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val AUTH_TOKEN = "auth_token"
        private const val UI_MODE = "ui_mode"
        private const val TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val USER_EMAIL = "user_email"
        private const val PLAN_EXPIRY = "plan_expiry"


        private const val SELECTED_APP_PACKAGES = "selected_app_packages"
    }

    // --- টোকেন ম্যানেজমেন্ট ---
    fun savePlanExpiry(expiryDate: String?) {
        if (expiryDate != null) {
            sharedPreferences.edit().putString(PLAN_EXPIRY, expiryDate).apply()
        }
    }

    fun getPlanExpiry(): String? {
        return sharedPreferences.getString(PLAN_EXPIRY, null)
    }

    fun isPlanExpired(): Boolean {
        val expiryStr = getPlanExpiry() ?: return true
        return try {
            val expiryTime = java.time.Instant.parse(expiryStr).toEpochMilli()
            System.currentTimeMillis() > expiryTime
        } catch (e: Exception) {
            true
        }
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(AUTH_TOKEN, null)
    }

    fun clearAuthToken() {
        // লগআউট হলে টেলিগ্রাম এবং অ্যাপ সিলেকশন ডেটাও মুছে ফেলা উচিত
        sharedPreferences.edit()
            .remove(AUTH_TOKEN)
            .remove(TELEGRAM_BOT_TOKEN)
            .remove(TELEGRAM_CHAT_ID)
            .remove(SELECTED_APP_PACKAGES) // <-- এটিও ক্লিয়ার করুন
            .apply()
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(USER_EMAIL, null)
    }

    fun saveUiMode(mode: UiMode) {
        sharedPreferences.edit().putString(UI_MODE, mode.name).apply()
    }

    fun getUiMode(): UiMode {
        val modeName = sharedPreferences.getString(UI_MODE, UiMode.SIMPLE.name)
        return UiMode.valueOf(modeName ?: UiMode.SIMPLE.name)
    }

    // --- টেলিগ্রাম ম্যানেজমেন্ট ---

    fun saveTelegramCredentials(token: String, chatId: String) {
        sharedPreferences.edit()
            .putString(TELEGRAM_BOT_TOKEN, token)
            .putString(TELEGRAM_CHAT_ID, chatId)
            .apply()
    }

    fun getTelegramBotToken(): String? {
        return sharedPreferences.getString(TELEGRAM_BOT_TOKEN, null)
    }

    fun getTelegramChatId(): String? {
        return sharedPreferences.getString(TELEGRAM_CHAT_ID, null)
    }

// --- !!! এই নতুন ফাংশনটি যোগ করা হয়েছে !!! ---
    /**
     * শুধুমাত্র টেলিগ্রামের তথ্য মুছে ফেলে।
     */
    fun clearTelegramCredentials() {
        sharedPreferences.edit()
            .remove(TELEGRAM_BOT_TOKEN)
            .remove(TELEGRAM_CHAT_ID)
            .apply()
    }
    // ------------------------------------------
    /**
     * ব্যবহারকারীর সিলেক্ট করা অ্যাপ প্যাকেজের তালিকা সেভ করে।
     * @param packages Set<String> যাতে প্যাকেজের নামগুলো আছে (e.g., "com.whatsapp")
     */
    fun saveSelectedAppPackages(packages: Set<String>) {
        sharedPreferences.edit().putStringSet(SELECTED_APP_PACKAGES, packages).apply()
    }

    /**
     * সেভ করা অ্যাপ প্যাকেজের তালিকা লোড করে।
     * @return Set<String>? তালিকা পাওয়া গেলে সেটি, অথবা খালি সেট (null-এর পরিবর্তে)।
     */
    fun getSelectedAppPackages(): Set<String> {
        // null রিটার্ন করার বদলে একটি খালি সেট রিটার্ন করা বেশি নিরাপদ
        return sharedPreferences.getStringSet(SELECTED_APP_PACKAGES, emptySet()) ?: emptySet()
    }
    // ---------------------------------------------------
}