package com.alsmsrecive.dev

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.utils.EncryptionUtil
import com.alsmsrecive.dev.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrivacyCenterActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private val api = ApiClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_privacy_center)

        session = SessionManager(applicationContext)

        // Apply window insets so content doesn't hide under status/nav bar
        val root = findViewById<LinearLayout>(R.id.rootPrivacy)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        // Set light icons if light mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isNightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            window.insetsController?.setSystemBarsAppearance(
                if (isNightMode) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }

        findViewById<ImageButton>(R.id.btnBackPrivacy).setOnClickListener { finish() }

        loadData()
    }

    private fun loadData() {
        val token = session.getAuthToken() ?: return
        val password = session.getUserPassword() ?: "default_pass"

        lifecycleScope.launch {
            try {
                val profileResp = api.getUserProfile(token)
                val msgResp = api.getMessages(token)

                // Populate account data
                if (profileResp.isSuccessful) {
                    val p = profileResp.body()!!
                    val id = p.email ?: p.phone ?: "Not set"
                    val planText = formatDate(p.planExpiresAt)
                    val devText = "${p.activeSessions} active / ${p.deviceLimit} allowed"
                    val statsText = "${p.stats.totalMessages} messages  •  ${p.stats.totalCallLogs} call logs  •  ${p.stats.trashedMessages} trashed"

                    findViewById<TextView>(R.id.tvProfileEmail).text = id
                    findViewById<TextView>(R.id.tvProfilePlanExpiry).text = planText
                    findViewById<TextView>(R.id.tvProfileDevices).text = devText
                    findViewById<TextView>(R.id.tvProfileStats).text = statsText
                }

                // Live flow proof
                val tvStep1 = findViewById<TextView>(R.id.tvStep1Raw)
                val tvStep3 = findViewById<TextView>(R.id.tvStep3Cipher)
                val tvStep4 = findViewById<TextView>(R.id.tvStep4Decrypted)

                if (msgResp.isSuccessful) {
                    val msgs = msgResp.body() ?: emptyList()

                    if (msgs.isNotEmpty()) {
                        // Prefer encrypted message, fallback to any message
                        val target = msgs.firstOrNull { it.message?.startsWith("U2FsdGVkX1") == true }
                            ?: msgs.first()

                        val rawMsg = target.message ?: ""
                        val rawSender = target.sender ?: "Unknown"
                        val isEncrypted = rawMsg.startsWith("U2FsdGVkX1")

                        val displaySender = if (isEncrypted) tryDecrypt(rawSender, password) else rawSender
                        val displayText = if (isEncrypted) tryDecrypt(rawMsg, password) else rawMsg

                        val senderSnippet = if (displaySender.length > 30) displaySender.take(30) + "..." else displaySender
                        val textSnippet = if (displayText.length > 60) displayText.take(60) + "..." else displayText

                        // Step 1: What the SMS actually says
                        tvStep1.text = "$senderSnippet: $textSnippet"

                        // Step 3: What hacker sees on server (raw encrypted / plain if old)
                        if (isEncrypted) {
                            tvStep3.text = if (rawMsg.length > 55) rawMsg.take(55) + "..." else rawMsg
                        } else {
                            tvStep3.text = "$rawSender: ${rawMsg.take(50)}...\n(This is old data, new messages will be encrypted)"
                        }

                        // Step 4: Same as step 1 — what you see
                        tvStep4.text = "$senderSnippet: $textSnippet"
                    } else {
                        tvStep1.text = "Alamin: Hello, how are you?"
                        tvStep3.text = "U2FsdGVkX19zXj3mK2pL..."
                        tvStep4.text = "Alamin: Hello, how are you?"
                    }
                }

            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvStep3Cipher).text = "Error: ${e.localizedMessage}"
                findViewById<TextView>(R.id.tvStep4Decrypted).text = "Check network connection"
            } finally {
                findViewById<LinearLayout>(R.id.layoutLoading).visibility = View.GONE
                findViewById<LinearLayout>(R.id.layoutContent).visibility = View.VISIBLE
            }
        }
    }

    private fun tryDecrypt(text: String, password: String): String {
        return try { EncryptionUtil.decrypt(text, password) } catch (e: Exception) { text }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "No active plan"
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            val date = fmt.parse(dateStr) ?: return dateStr
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) { dateStr }
    }
}
