package com.alsmsrecive.dev

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.network.models.TelegramSettingsRequest
import com.alsmsrecive.dev.utils.EncryptionUtil
import com.alsmsrecive.dev.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class TelegramSettingsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etBotToken: TextInputEditText
    private lateinit var etChatId: TextInputEditText
    private lateinit var btnSaveTelegram: Button
    private lateinit var btnClearTelegram: Button
    private lateinit var btnTelegramBack: ImageButton
    private lateinit var switchTelegramSync: androidx.appcompat.widget.SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)

        sessionManager = SessionManager(applicationContext)

        // UI এলিমেন্টগুলো লিঙ্ক করুন
        etBotToken = findViewById(R.id.etBotToken)
        etChatId = findViewById(R.id.etChatId)
        btnSaveTelegram = findViewById(R.id.btnSaveTelegram)
        btnClearTelegram = findViewById(R.id.btnClearTelegram)
        btnTelegramBack = findViewById(R.id.btnTelegramBack)
        switchTelegramSync = findViewById(R.id.switchTelegramSync)

        // সেভ করা ডেটা লোড করুন
        loadSavedCredentials()

        // বাটন লিসেনার সেট করুন
        btnTelegramBack.setOnClickListener {
            finish() // এই পেজটি বন্ধ করুন
        }

        btnSaveTelegram.setOnClickListener {
            handleSaveTelegramCredentials()
        }

        btnClearTelegram.setOnClickListener {
            handleClearTelegramCredentials()
        }
    }

    private fun loadSavedCredentials() {
        etBotToken.setText(sessionManager.getTelegramBotToken())
        etChatId.setText(sessionManager.getTelegramChatId())
        switchTelegramSync.isChecked = sessionManager.isTelegramForwardingEnabled()
    }

    private fun handleSaveTelegramCredentials() {
        val botToken = etBotToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()

        if (botToken.isEmpty() || chatId.isEmpty()) {
            showToast("Please enter both Bot Token and Chat ID")
            return
        }

        // 1. Save Locally
        sessionManager.saveTelegramCredentials(botToken, chatId)
        sessionManager.setTelegramForwardingEnabled(switchTelegramSync.isChecked)

        // 2. Encrypt and save to Server
        val token = sessionManager.getAuthToken()
        val password = sessionManager.getUserPassword()
        if (token != null && password != null) {
            val encToken = EncryptionUtil.encrypt(botToken, password)
            val encChatId = EncryptionUtil.encrypt(chatId, password)
            
            lifecycleScope.launch {
                try {
                    ApiClient.instance.saveTelegramSettings(token, TelegramSettingsRequest(encToken, encChatId))
                    showToast("Telegram credentials synced to cloud!")
                    finish()
                } catch (e: Exception) {
                    showToast("Saved locally, but failed to sync to cloud.")
                    finish()
                }
            }
        } else {
            showToast("Telegram credentials saved locally!")
            finish()
        }
    }

    private fun handleClearTelegramCredentials() {
        sessionManager.clearTelegramCredentials()
        etBotToken.setText("")
        etChatId.setText("")

        val token = sessionManager.getAuthToken()
        if (token != null) {
            lifecycleScope.launch {
                try {
                    ApiClient.instance.saveTelegramSettings(token, TelegramSettingsRequest("", ""))
                    showToast("Telegram credentials cleared from cloud!")
                } catch (e: Exception) {
                    showToast("Cleared locally.")
                }
            }
        } else {
            showToast("Telegram credentials cleared locally!")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}