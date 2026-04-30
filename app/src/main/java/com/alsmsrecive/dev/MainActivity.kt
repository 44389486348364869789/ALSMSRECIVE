package com.alsmsrecive.dev

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alsmsrecive.dev.adapter.MessageAdapter
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.network.TrashRequest
import com.alsmsrecive.dev.network.models.LoginRequest
import com.alsmsrecive.dev.network.models.LogoutRequest
import com.alsmsrecive.dev.network.models.MessageResponse
import com.alsmsrecive.dev.network.models.RegisterRequest
import com.alsmsrecive.dev.repository.MessageRepository
import com.alsmsrecive.dev.utils.SessionManager
import com.alsmsrecive.dev.utils.UiMode
import com.alsmsrecive.dev.utils.EncryptionUtil
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance

    private lateinit var loginView: View
    private lateinit var securityHubView: View
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBarLogin: ProgressBar
    private lateinit var btnSync: Button
    private lateinit var btnMenu: ImageButton
    private lateinit var btnFilterDevice: ImageButton
    private lateinit var progressBarSync: ProgressBar
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var btnGoToDisguiseMode: Button
    private lateinit var bottomActionBar: LinearLayout
    private lateinit var btnDeleteSelected: Button

    // !!! Variable Declared Correctly !!!
    private lateinit var layoutEmptyState: LinearLayout

    private var registerDialog: AlertDialog? = null
    private lateinit var messageAdapter: MessageAdapter
    private var messageList = mutableListOf<MessageResponse>()
    private var allMessagesList = mutableListOf<MessageResponse>()
    private var currentDeviceFilter: String? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.RECEIVE_SMS] == true) showToast("SMS permission granted")
        if (permissions[Manifest.permission.READ_CALL_LOG] == true) { showToast("Call Log permission granted. Syncing..."); syncCallLogs() }
    }

    private val notificationAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val newMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MessageRepository.ACTION_NEW_MESSAGE_RECEIVED) {
                syncMessagesFromServer()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sessionManager = SessionManager(applicationContext)
        bindViews()
        checkTokenAndSetupViews()
        fetchActiveBroadcast()
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.getAuthToken() != null && sessionManager.getUiMode() == UiMode.SECURITY) {
            checkPlanStatus()
            if (!sessionManager.isPlanExpired()) {
                syncMessagesFromServer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sessionManager.getAuthToken() != null && sessionManager.getUiMode() == UiMode.SECURITY) {
            try { LocalBroadcastManager.getInstance(this).unregisterReceiver(newMessageReceiver) } catch (e: Exception) {}
        }
        registerDialog?.dismiss()
    }

    private fun checkPlanStatus() {
        if (sessionManager.isPlanExpired()) {
            AlertDialog.Builder(this)
                .setTitle("Plan Expired! ❌")
                .setMessage("Your free trial or subscription has ended. Please renew to continue.")
                .setCancelable(false)
                .setPositiveButton("Buy Plan") { _, _ ->
                    startActivity(Intent(this, PlanActivity::class.java))
                }
                .setNegativeButton("Close App") { _, _ ->
                    finishAffinity()
                }
                .show()
        }
    }

    private fun bindViews() {
        loginView = findViewById(R.id.login_view)
        securityHubView = findViewById(R.id.security_hub_view)
        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBarLogin = findViewById(R.id.progressBar)
        btnSync = findViewById(R.id.btnSync)
        btnMenu = findViewById(R.id.btnMenu)
        btnFilterDevice = findViewById(R.id.btnFilterDevice)
        progressBarSync = findViewById(R.id.progressBarSync)
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        btnGoToDisguiseMode = findViewById(R.id.btnGoToDisguiseMode)
        bottomActionBar = findViewById(R.id.bottom_action_bar_main)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)

        // !!! Variable Initialized Correctly !!!
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
    }

    private fun checkTokenAndSetupViews() {
        val token = sessionManager.getAuthToken()
        if (token == null) {
            sessionManager.saveUiMode(UiMode.SIMPLE)
            showLoginView()
        } else {
            if (sessionManager.getUiMode() == UiMode.SIMPLE) goToSimpleModeActivityAndFinish()
            else {
                // Ensure device info is captured for existing users who didn't re-login
                if (sessionManager.getDeviceId() == "unknown") {
                    sessionManager.saveDeviceInfo(getUniqueDeviceId(), getDeviceName())
                }
                
                showSecurityHubView()
                LocalBroadcastManager.getInstance(this).registerReceiver(newMessageReceiver, IntentFilter(MessageRepository.ACTION_NEW_MESSAGE_RECEIVED))
            }
        }
    }

    private fun showLoginView() {
        loginView.visibility = View.VISIBLE
        securityHubView.visibility = View.GONE
        btnLogin.setOnClickListener { handleLogin() }
        tvRegister.setOnClickListener { showRegisterDialog() }
    }

    private fun showSecurityHubView() {
        loginView.visibility = View.GONE
        securityHubView.visibility = View.VISIBLE
        setupRecyclerView()
        btnSync.setOnClickListener {
            checkPlanStatus()
            if(!sessionManager.isPlanExpired()) {
                syncMessagesFromServer()
                if (hasCallLogPermission()) syncCallLogs()
                else showToast("Syncing messages... (Call Log permission not granted)")
            }
        }
        btnGoToDisguiseMode.setOnClickListener { handleGoToDisguiseMode() }
        btnMenu.setOnClickListener { showHubMenu(it) }
        btnFilterDevice.setOnClickListener { showDeviceFilterMenu(it) }
        btnDeleteSelected.setOnClickListener { handleTrashSelectedItems() }
    }

    private fun showHubMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.hub_menu, popup.menu)

        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mPopup, true)
        } catch (e: Exception) {}

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_plan -> { startActivity(Intent(this, PlanActivity::class.java)); true }
                R.id.menu_sms_perm -> { requestSmsPermission(); true }
                R.id.menu_notif_perm -> { requestNotificationAccess(); true }
                R.id.menu_select_apps -> { startActivity(Intent(this, AppSelectionActivity::class.java)); true }
                R.id.menu_call_log_perm -> { if (hasCallLogPermission()) { syncCallLogs(); startActivity(Intent(this, CallLogActivity::class.java)) } else requestCallLogPermission(); true }
                R.id.menu_telegram_settings -> { startActivity(Intent(this, TelegramSettingsActivity::class.java)); true }
                R.id.menu_trash -> { startActivity(Intent(this, TrashActivity::class.java)); true }
                R.id.menu_clear_all -> { confirmDeleteAllMessages_First(); true }
                R.id.menu_help -> { startActivity(Intent(this, HelpActivity::class.java)); true }
                R.id.menu_logout -> { handleLogout(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeviceFilterMenu(view: View) {
        val popup = PopupMenu(this, view)
        // Add "All Devices" default option
        popup.menu.add(0, 0, 0, "All Devices")
        
        // Find unique devices from allMessagesList, excluding "Unknown Device" and "unknown"
        val uniqueDevices = allMessagesList.mapNotNull { it.deviceName }
            .filter { it != "Unknown Device" && it != "unknown" }
            .distinct()
        
        uniqueDevices.forEachIndexed { index, deviceName ->
            popup.menu.add(0, index + 1, 0, deviceName)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == 0) {
                currentDeviceFilter = null
            } else {
                currentDeviceFilter = menuItem.title.toString()
            }
            applyDeviceFilter()
            true
        }
        popup.show()
    }

    private fun handleLogin() {
        val identifier = etEmail.text.toString().trim()
        val password = etPass.text.toString().trim()
        if (identifier.isEmpty() || password.isEmpty()) { showToast("Required fields empty"); return }

        showLoading(true, progressBarLogin, btnLogin)
        lifecycleScope.launch {
            try {
                val request = LoginRequest(identifier, password, getUniqueDeviceId(), getDeviceName())
                val response = apiService.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveAuthToken(body.token)
                    sessionManager.saveUserEmail(identifier)
                    sessionManager.savePlanExpiry(body.planExpiresAt)
                    sessionManager.saveUiMode(UiMode.SECURITY)
                    sessionManager.saveUserPassword(password) // এনক্রিপশনের জন্য সেভ
                    sessionManager.saveDeviceInfo(getUniqueDeviceId(), getDeviceName()) // ডিভাইসের নাম ও আইডি সেভ
                    
                    // টেলিগ্রাম টোকেন সিঙ্ক (ডিক্রিপ্ট করে সেভ করা)
                    if (!body.telegramBotToken.isNullOrEmpty() && !body.telegramChatId.isNullOrEmpty()) {
                        val decBotToken = EncryptionUtil.decrypt(body.telegramBotToken, password)
                        val decChatId = EncryptionUtil.decrypt(body.telegramChatId, password)
                        sessionManager.saveTelegramCredentials(decBotToken, decChatId)
                    }

                    showToast("Login Successful!")
                    showSecurityHubView()
                    LocalBroadcastManager.getInstance(this@MainActivity).registerReceiver(newMessageReceiver, IntentFilter(MessageRepository.ACTION_NEW_MESSAGE_RECEIVED))
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (response.code() == 406) {
                        try {
                            val json = org.json.JSONObject(errorBody ?: "")
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Login Failed")
                                .setMessage(json.optString("msg", "Limit Reached") + "\n\n" + json.optString("details", ""))
                                .setPositiveButton("OK", null).show()
                        } catch (e: Exception) { showToast("Limit Reached") }
                    } else if(response.code() == 403) showToast("Account Blocked")
                    else showToast("Invalid Credentials")
                }
            } catch (e: Exception) { showToast("Error: ${e.message}") }
            finally { showLoading(false, progressBarLogin, btnLogin) }
        }
    }

    private fun showRegisterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register, null)
        val etRegisterEmail = dialogView.findViewById<TextInputEditText>(R.id.etRegisterEmail)
        val etRegisterPhone = dialogView.findViewById<TextInputEditText>(R.id.etRegisterPhone)
        val etRegisterPassword = dialogView.findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val btnDialogRegister = dialogView.findViewById<Button>(R.id.btnDialogRegister)
        val btnDialogCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)

        registerDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        registerDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnDialogRegister.setOnClickListener {
            val email = etRegisterEmail.text.toString().trim()
            val phone = etRegisterPhone.text.toString().trim()
            val password = etRegisterPassword.text.toString().trim()
            if ((email.isEmpty() && phone.isEmpty()) || password.isEmpty()) showToast("Provide Email OR Phone, and Password")
            else handleRegisterSubmit(if(email.isNotEmpty()) email else null, if(phone.isNotEmpty()) phone else null, password, btnDialogRegister)
        }
        btnDialogCancel.setOnClickListener { registerDialog?.dismiss() }
        registerDialog?.show()
    }

    private fun handleRegisterSubmit(email: String?, phone: String?, password: String, btn: Button) {
        showLoading(true, progressBarLogin, btn)
        lifecycleScope.launch {
            try {
                val response = apiService.registerUser(RegisterRequest(email, phone, password))
                if (response.isSuccessful) { showToast("Registered! Login now."); registerDialog?.dismiss() }
                else showToast("Failed: " + (response.errorBody()?.string() ?: "Error"))
            } catch (e: Exception) { showToast("Error: ${e.message}") }
            finally { showLoading(false, progressBarLogin, btn) }
        }
    }

    private fun handleLogout() {
        showLoading(true, progressBarSync, btnSync)
        lifecycleScope.launch {
            try { apiService.logoutUser(LogoutRequest(sessionManager.getUserEmail() ?: "", getUniqueDeviceId())) } catch (e: Exception) {}
            sessionManager.clearAuthToken()
            sessionManager.saveUiMode(UiMode.SIMPLE)
            showToast("Logged out")
            try { LocalBroadcastManager.getInstance(this@MainActivity).unregisterReceiver(newMessageReceiver) } catch (e: Exception) {}
            bottomActionBar.visibility = View.GONE
            if (::messageAdapter.isInitialized) messageAdapter.clearSelection()
            showLoading(false, progressBarSync, btnSync)
            showLoginView()
        }
    }

    private fun handleGoToDisguiseMode() { sessionManager.saveUiMode(UiMode.SIMPLE); goToSimpleModeActivityAndFinish() }
    private fun goToSimpleModeActivityAndFinish() { startActivity(Intent(this, SimpleModeActivity::class.java)); finish() }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, isSelectable = true)
        recyclerViewMessages.adapter = messageAdapter
        recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        messageAdapter.setOnSelectionListener { count ->
            bottomActionBar.visibility = if (count > 0) View.VISIBLE else View.GONE
            btnDeleteSelected.text = "Move to Trash ($count)"
        }
    }

    fun syncMessagesFromServer() {
        showLoading(true, progressBarSync, btnSync)
        val token = sessionManager.getAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getMessages(token)
                if (response.isSuccessful) {
                    messageList.clear()
                    val newMessages = response.body() ?: emptyList()
                    
                    val password = sessionManager.getUserPassword()
                    val decryptedMessages = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        newMessages.map { msg ->
                            msg.copy(
                                message = EncryptionUtil.decrypt(msg.message, password),
                                sender = EncryptionUtil.decrypt(msg.sender, password)
                            )
                        }
                    }

                    allMessagesList.clear()
                    allMessagesList.addAll(decryptedMessages)
                    applyDeviceFilter()
                    
                } else handleApiError(response.code())
            } catch (e: Exception) {
                showToast("Sync failed: ${e.message}")
            } finally {
                showLoading(false, progressBarSync, btnSync)
            }
        }
    }

    private fun applyDeviceFilter() {
        messageList.clear()
        if (currentDeviceFilter == null) {
            messageList.addAll(allMessagesList)
        } else {
            messageList.addAll(allMessagesList.filter { it.deviceName == currentDeviceFilter })
        }
        
        messageAdapter.notifyDataSetChanged()

        if (messageList.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerViewMessages.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerViewMessages.visibility = View.VISIBLE
        }

        bottomActionBar.visibility = View.GONE
        messageAdapter.clearSelection()
    }

    private fun handleTrashSelectedItems() {
        val selectedIds = messageAdapter.getSelectedItems()
        if (selectedIds.isEmpty()) return
        showLoading(true, progressBarSync, btnSync)
        lifecycleScope.launch {
            try {
                if (apiService.trashMessagesByIds(sessionManager.getAuthToken()!!, TrashRequest(selectedIds)).isSuccessful) {
                    showToast("Trashed"); syncMessagesFromServer()
                }
            } catch (e: Exception) { showToast("Error") } finally { showLoading(false, progressBarSync, btnSync) }
        }
    }

    private fun confirmDeleteAllMessages_First() { AlertDialog.Builder(this).setTitle("Delete All?").setPositiveButton("Yes") { _, _ -> handleDeleteAllMessages() }.setNegativeButton("Cancel", null).show() }

    private fun handleDeleteAllMessages() {
        showLoading(true, progressBarSync, btnSync)
        lifecycleScope.launch {
            try {
                if (apiService.deleteAllMessages(sessionManager.getAuthToken()!!).isSuccessful) {
                    showToast("All trashed"); syncMessagesFromServer()
                }
            } catch (e: Exception) { showToast("Error") } finally { showLoading(false, progressBarSync, btnSync) }
        }
    }

    private fun fetchActiveBroadcast() {
        lifecycleScope.launch {
            try {
                val response = apiService.getActiveBroadcast()
                if (response.isSuccessful && response.body() != null) {
                    val broadcast = response.body()!!
                    val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    if (sessionManager.canShowBroadcast(broadcast._id, currentDate)) {
                        showBroadcastDialog(broadcast)
                        sessionManager.incrementBroadcastViewCount()
                    }
                }
            } catch (e: Exception) {
                // Ignore silent failure for broadcast
            }
        }
    }

    private fun showBroadcastDialog(broadcast: com.alsmsrecive.dev.network.models.BroadcastResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_broadcast, null)
        val ivBroadcastImage = dialogView.findViewById<ImageView>(R.id.ivBroadcastImage)
        val tvBroadcastTitle = dialogView.findViewById<TextView>(R.id.tvBroadcastTitle)
        val tvBroadcastMessage = dialogView.findViewById<TextView>(R.id.tvBroadcastMessage)
        
        tvBroadcastTitle.text = broadcast.title
        tvBroadcastMessage.text = broadcast.message
        
        if (!broadcast.imageUrl.isNullOrEmpty()) {
            ivBroadcastImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(broadcast.imageUrl)
                .centerCrop()
                .into(ivBroadcastImage)
        } else {
            ivBroadcastImage.visibility = View.GONE
        }

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            
        if (!broadcast.link.isNullOrEmpty()) {
            val btnText = if (!broadcast.linkText.isNullOrEmpty()) broadcast.linkText else "Open Link"
            builder.setPositiveButton(btnText) { dialog, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(broadcast.link))
                    startActivity(intent)
                } catch (e: Exception) {
                    showToast("Cannot open link")
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
        } else {
            builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        }
        
        builder.show()
    }

    private fun hasSmsPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    private fun requestSmsPermission() = requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
    private fun hasNotificationAccess() = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
    private fun requestNotificationAccess() = notificationAccessLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    private fun hasCallLogPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    private fun requestCallLogPermission() = requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG))

    private fun syncCallLogs() {
        MessageRepository.syncCallLogs(applicationContext, sessionManager.getAuthToken()!!) { success, msg -> if(!success) handleApiError(if(msg.contains("403")) 403 else 0) }
    }

    private fun getUniqueDeviceId() = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    private fun getDeviceName() = Build.MODEL ?: "Android"
    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun showLoading(isLoading: Boolean, bar: ProgressBar, btn: Button?) { bar.visibility = if (isLoading) View.VISIBLE else View.GONE; btn?.isEnabled = !isLoading }

    private fun handleApiError(code: Int) {
        if (code == 403) { showToast("Blocked"); runOnUiThread { handleLogout() } }
        else if (code == 401) { showToast("Session Expired"); runOnUiThread { handleLogout() } }
        else if (code == 402) { checkPlanStatus() }
    }
}