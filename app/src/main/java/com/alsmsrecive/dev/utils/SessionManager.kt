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
        private const val USER_PHONE = "user_phone"
        private const val PLAN_EXPIRY = "plan_expiry"
        private const val LOGIN_PASSWORD = "login_password"

        private const val SELECTED_APP_PACKAGES = "selected_app_packages"
        private const val DEVICE_ID = "device_id"
        private const val DEVICE_NAME = "device_name"
        private const val TELEGRAM_FORWARDING_ENABLED = "telegram_forwarding_enabled"
        private const val DEVICE_LIMIT = "device_limit"
        private const val ACTIVE_SESSIONS = "active_sessions"
    }

    // --- টোকেন ম্যানেজমেন্ট ---
    fun savePlanExpiry(expiryDate: String?) {
        if (expiryDate != null) {
            sharedPreferences.edit().putString(PLAN_EXPIRY, expiryDate).apply()
        }
    }

    fun saveDeviceLimits(limit: Int, active: Int) {
        sharedPreferences.edit()
            .putInt(DEVICE_LIMIT, limit)
            .putInt(ACTIVE_SESSIONS, active)
            .apply()
    }

    fun getDeviceLimit(): Int = sharedPreferences.getInt(DEVICE_LIMIT, 1)
    fun getActiveSessionsCount(): Int = sharedPreferences.getInt(ACTIVE_SESSIONS, 0)

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
        sharedPreferences.edit()
            .remove(AUTH_TOKEN)
            .remove(TELEGRAM_BOT_TOKEN)
            .remove(TELEGRAM_CHAT_ID)
            .remove(SELECTED_APP_PACKAGES)
            .remove(LOGIN_PASSWORD)
            .remove(USER_EMAIL)       // Clear on logout — prevent stale identifier
            .remove(USER_PHONE)       // Clear on logout — prevent stale identifier
            .remove(PLAN_EXPIRY)
            .remove(DEVICE_LIMIT)
            .remove(ACTIVE_SESSIONS)
            .apply()
    }

    // --- এনক্রিপশন পাসওয়ার্ড ---
    fun saveUserPassword(password: String) {
        sharedPreferences.edit().putString(LOGIN_PASSWORD, password).apply()
    }

    fun getUserPassword(): String? {
        return sharedPreferences.getString(LOGIN_PASSWORD, null)
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(USER_EMAIL, null)
    }

    fun saveUserPhone(phone: String) {
        sharedPreferences.edit().putString(USER_PHONE, phone).apply()
    }

    fun getUserPhone(): String? {
        return sharedPreferences.getString(USER_PHONE, null)
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

    // --- Device Info ---
    fun saveDeviceInfo(deviceId: String, deviceName: String) {
        sharedPreferences.edit()
            .putString(DEVICE_ID, deviceId)
            .putString(DEVICE_NAME, deviceName)
            .apply()
    }

    fun getDeviceId(): String {
        return sharedPreferences.getString(DEVICE_ID, "unknown") ?: "unknown"
    }

    fun getDeviceName(): String {
        return sharedPreferences.getString(DEVICE_NAME, "Unknown Device") ?: "Unknown Device"
    }

    // --- Telegram Forwarding Toggle ---
    fun setTelegramForwardingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(TELEGRAM_FORWARDING_ENABLED, enabled).apply()
    }

    fun isTelegramForwardingEnabled(): Boolean {
        return sharedPreferences.getBoolean(TELEGRAM_FORWARDING_ENABLED, true) // Default true
    }

    // --- Broadcast Tracking ---
    fun canShowBroadcast(broadcastId: String, currentDate: String): Boolean {
        val savedId = sharedPreferences.getString("LAST_BROADCAST_ID", "")
        val savedDate = sharedPreferences.getString("LAST_BROADCAST_DATE", "")
        var viewCount = sharedPreferences.getInt("BROADCAST_VIEW_COUNT", 0)

        if (savedId != broadcastId || savedDate != currentDate) {
            // New broadcast or new day -> reset count
            sharedPreferences.edit()
                .putString("LAST_BROADCAST_ID", broadcastId)
                .putString("LAST_BROADCAST_DATE", currentDate)
                .putInt("BROADCAST_VIEW_COUNT", 0)
                .apply()
            viewCount = 0
        }
        
        return viewCount < 3
    }

    fun incrementBroadcastViewCount() {
        val currentCount = sharedPreferences.getInt("BROADCAST_VIEW_COUNT", 0)
        sharedPreferences.edit()
            .putInt("BROADCAST_VIEW_COUNT", currentCount + 1)
            .apply()
    }
}