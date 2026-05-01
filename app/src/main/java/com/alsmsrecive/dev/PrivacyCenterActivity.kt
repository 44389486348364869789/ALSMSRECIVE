package com.alsmsrecive.dev

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.utils.EncryptionUtil
import com.alsmsrecive.dev.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrivacyCenterActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance

    private lateinit var btnBack: ImageButton
    private lateinit var tvServerView: TextView
    private lateinit var tvUserView: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePlanExpiry: TextView
    private lateinit var tvProfileDevices: TextView
    private lateinit var tvProfileStats: TextView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_center)

        sessionManager = SessionManager(applicationContext)

        btnBack = findViewById(R.id.btnBackPrivacy)
        tvServerView = findViewById(R.id.tvServerView)
        tvUserView = findViewById(R.id.tvUserView)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfilePlanExpiry = findViewById(R.id.tvProfilePlanExpiry)
        tvProfileDevices = findViewById(R.id.tvProfileDevices)
        tvProfileStats = findViewById(R.id.tvProfileStats)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutContent = findViewById(R.id.layoutContent)

        btnBack.setOnClickListener { finish() }

        fetchAllData()
    }

    private fun fetchAllData() {
        val token = sessionManager.getAuthToken() ?: return
        val password = sessionManager.getUserPassword() ?: "default_pass"

        lifecycleScope.launch {
            try {
                // Fetch profile and messages in parallel logic (sequential for simplicity)
                val profileResponse = apiService.getUserProfile(token)
                val messagesResponse = apiService.getMessages(token)

                if (profileResponse.isSuccessful) {
                    val profile = profileResponse.body()!!

                    // Email/Phone
                    val identifier = profile.email ?: profile.phone ?: "Not set"
                    tvProfileEmail.text = identifier

                    // Plan Expiry
                    tvProfilePlanExpiry.text = formatDate(profile.planExpiresAt)

                    // Devices
                    tvProfileDevices.text = "${profile.activeSessions} active / ${profile.deviceLimit} allowed"

                    // Stats
                    val stats = profile.stats
                    tvProfileStats.text = "${stats.totalMessages} messages • ${stats.totalCallLogs} call logs • ${stats.trashedMessages} in trash"
                }

                // E2EE Proof
                if (messagesResponse.isSuccessful) {
                    val messages = messagesResponse.body() ?: emptyList()

                    val encryptedMsg = messages.firstOrNull { it.message?.startsWith("U2FsdGVkX1") == true }

                    if (encryptedMsg != null) {
                        val rawMsg = encryptedMsg.message ?: ""
                        val rawSender = encryptedMsg.sender ?: ""

                        val decryptedSender = try { EncryptionUtil.decrypt(rawSender, password) } catch (e: Exception) { rawSender }
                        val decryptedText = try { EncryptionUtil.decrypt(rawMsg, password) } catch (e: Exception) { "Decryption failed" }

                        val serverSnippet = if (rawMsg.length > 60) rawMsg.take(60) + "..." else rawMsg
                        val senderSnippet = if (rawSender.length > 20) rawSender.take(20) + "..." else rawSender

                        tvServerView.text = "{\n  \"sender\": \"$senderSnippet\",\n  \"message\": \"$serverSnippet\"\n}"
                        tvUserView.text = "From: $decryptedSender\n\n$decryptedText"
                    } else if (messages.isNotEmpty()) {
                        // Messages exist but not yet encrypted (old data)
                        val first = messages.first()
                        tvServerView.text = "{\n  \"sender\": \"${first.sender}\",\n  \"message\": \"${first.message?.take(60)}...\"\n}"
                        tvUserView.text = "⚠️ This message was received before E2EE was enabled.\nNew messages will be fully encrypted."
                    } else {
                        tvServerView.text = "{ \"message\": \"No messages on server yet\" }"
                        tvUserView.text = "📩 Send an SMS to your phone and it will appear here as proof."
                    }
                }

            } catch (e: Exception) {
                tvServerView.text = "Error: ${e.localizedMessage}"
                tvUserView.text = "Could not reach server."
            } finally {
                layoutLoading.visibility = View.GONE
                layoutContent.visibility = View.VISIBLE
            }
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "No plan"
        return try {
            val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFmt.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFmt.parse(dateStr) ?: return dateStr
            val outFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            outFmt.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
