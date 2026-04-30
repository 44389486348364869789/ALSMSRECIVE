package com.example.alsmsrecive

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alsmsrecive.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText

class TelegramSettingsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etBotToken: TextInputEditText
    private lateinit var etChatId: TextInputEditText
    private lateinit var btnSaveTelegram: Button
    private lateinit var btnClearTelegram: Button
    private lateinit var btnTelegramBack: ImageButton

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
    }

    private fun handleSaveTelegramCredentials() {
        val botToken = etBotToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()

        if (botToken.isEmpty() || chatId.isEmpty()) {
            showToast("Please enter both Bot Token and Chat ID")
            return
        }

        sessionManager.saveTelegramCredentials(botToken, chatId)
        showToast("Telegram credentials saved!")
        finish() // সেভ করার পর পেজটি বন্ধ করে দিন
    }

    private fun handleClearTelegramCredentials() {
        sessionManager.clearTelegramCredentials()
        etBotToken.setText("")
        etChatId.setText("")
        showToast("Telegram credentials cleared!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}