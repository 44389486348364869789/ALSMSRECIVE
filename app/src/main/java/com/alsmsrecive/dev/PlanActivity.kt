package com.alsmsrecive.dev

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.network.ApiService
import com.alsmsrecive.dev.network.models.CreateOrderRequest
import com.alsmsrecive.dev.network.models.VerifyOrderRequest
import com.alsmsrecive.dev.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PlanActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var tvPlanStatus: TextView
    private lateinit var tvExpiryDate: TextView
    private lateinit var tvTimeLeft: TextView
    private lateinit var tvDeviceLimits: TextView
    private lateinit var btnBack: Button

    private var paymentInfo: com.alsmsrecive.dev.network.models.PaymentInfoResponse? = null
    private var expiryTimeMillis: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateCountdown()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)
        
        sessionManager = SessionManager(applicationContext)
        apiService = ApiClient.instance

        tvPlanStatus = findViewById(R.id.tvPlanStatus)
        tvExpiryDate = findViewById(R.id.tvExpiryDate)
        tvTimeLeft = findViewById(R.id.tvTimeLeft)
        tvDeviceLimits = findViewById(R.id.tvDeviceLimits)
        btnBack = findViewById(R.id.btnBackPlan)

        findViewById<MaterialCardView>(R.id.cardPlan18).setOnClickListener { showBuyPlanSheet(18) }
        findViewById<MaterialCardView>(R.id.cardPlan29).setOnClickListener { showBuyPlanSheet(29) }
        findViewById<MaterialCardView>(R.id.cardPlan49).setOnClickListener { showBuyPlanSheet(49) }
        findViewById<MaterialCardView>(R.id.cardPlan99).setOnClickListener { showBuyPlanSheet(99) }

        btnBack.setOnClickListener { finish() }

        displayPlanInfo()
        fetchPaymentInfo()
    }

    private fun fetchPaymentInfo() {
        lifecycleScope.launch {
            try {
                val res = apiService.getPaymentInfo()
                if (res.isSuccessful && res.body() != null) {
                    paymentInfo = res.body()
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(countdownRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun displayPlanInfo() {
        val expiryStr = sessionManager.getPlanExpiry()
        
        val active = sessionManager.getActiveSessionsCount()
        val max = sessionManager.getDeviceLimit()
        tvDeviceLimits.text = "Active Devices: $active / $max"

        if (expiryStr != null) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(expiryStr)
                
                if (date != null) {
                    expiryTimeMillis = date.time
                    val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    tvExpiryDate.text = "Exact Date: ${outputFormat.format(date)}"
                }

                if (sessionManager.isPlanExpired()) {
                    tvPlanStatus.text = "Status: EXPIRED"
                    tvPlanStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    tvTimeLeft.text = "Expired"
                    tvTimeLeft.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                } else {
                    tvPlanStatus.text = "Status: ACTIVE"
                    tvPlanStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    updateCountdown() // Update immediately
                }
            } catch (e: Exception) {
                tvExpiryDate.text = "Exact Date: $expiryStr"
            }
        } else {
            tvExpiryDate.text = "Exact Date: N/A"
            tvPlanStatus.text = "Status: UNKNOWN"
            tvTimeLeft.text = "N/A"
        }
    }

    private fun updateCountdown() {
        if (expiryTimeMillis == 0L) return
        val now = System.currentTimeMillis()
        val diff = expiryTimeMillis - now

        if (diff <= 0) {
            tvTimeLeft.text = "Expired"
            tvTimeLeft.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            return
        }

        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        val mins = (diff / (1000 * 60)) % 60
        val secs = (diff / 1000) % 60

        tvTimeLeft.text = "Expires in: $days Days $hours Hrs $mins Mins $secs Secs"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showBuyPlanSheet(planType: Int) {
        val maxDevices = sessionManager.getDeviceLimit()
        val requestedDevices = when (planType) {
            18 -> 1
            29 -> 2
            49 -> 5
            99 -> 21
            else -> 1
        }
        
        if (!sessionManager.isPlanExpired() && requestedDevices < maxDevices) {
            showToast("You cannot downgrade your plan while it is still active.")
            return
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_buy_plan, null)
        bottomSheetDialog.setContentView(view)

        val tvSheetTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val rgPaymentMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val tvInstructions = view.findViewById<TextView>(R.id.tvInstructions)
        val tilTrxId = view.findViewById<TextInputLayout>(R.id.tilTrxId)
        val etTrxId = view.findViewById<TextInputEditText>(R.id.etTrxId)
        val btnSubmitOrder = view.findViewById<Button>(R.id.btnSubmitOrder)
        
        val rbBkash = view.findViewById<RadioButton>(R.id.rbBkash)
        val rbNagad = view.findViewById<RadioButton>(R.id.rbNagad)
        val rbBinance = view.findViewById<RadioButton>(R.id.rbBinance)

        if (paymentInfo != null) {
            rbBkash.text = "bKash (Personal) - ${paymentInfo!!.bkash}"
            rbNagad.text = "Nagad (Personal) - ${paymentInfo!!.nagad}"
            rbBinance.text = "Binance Pay - Pay ID: ${paymentInfo!!.binance}"
        }
        
        var selectedMethod = ""
        var currentOrderId = ""
        var currentReferenceId = ""

        tvSheetTitle.text = "Buy Plan ($planType BDT)"

        rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            selectedMethod = when (checkedId) {
                R.id.rbBkash -> "bkash"
                R.id.rbNagad -> "nagad"
                R.id.rbBinance -> "binance"
                else -> ""
            }
            
            if (selectedMethod.isNotEmpty()) {
                tvInstructions.visibility = View.VISIBLE
                tilTrxId.visibility = View.GONE
                btnSubmitOrder.isEnabled = false
                btnSubmitOrder.text = "Creating Order..."
                tvInstructions.text = "Please wait..."

                // Call create order
                lifecycleScope.launch {
                    try {
                        val token = sessionManager.getAuthToken() ?: return@launch
                        val req = CreateOrderRequest(planType, selectedMethod)
                        val res = apiService.createOrder(token, req)
                        if (res.isSuccessful && res.body() != null) {
                            currentOrderId = res.body()!!.orderId
                            currentReferenceId = res.body()!!.referenceId
                            
                            val instructionText = if (selectedMethod == "binance") {
                                "Send exactly ${res.body()!!.amountUSDT} USDT to Binance Pay ID. \n\nIMPORTANT: Enter Reference ${currentReferenceId} in the Note/Remark.\n\nAfter paying, click Verify."
                            } else {
                                "Send exactly $planType BDT to the number above via 'Send Money'. \n\nIMPORTANT: Enter Reference ${currentReferenceId} during payment.\n\nAfter paying, click Verify."
                            }
                            tvInstructions.text = instructionText

                            btnSubmitOrder.isEnabled = true
                            btnSubmitOrder.text = "Verify Payment"
                        } else {
                            showToast("Failed to create order")
                            bottomSheetDialog.dismiss()
                        }
                    } catch (e: Exception) {
                        showToast("Error creating order")
                        bottomSheetDialog.dismiss()
                    }
                }
            }
        }

        btnSubmitOrder.setOnClickListener {
            if (currentOrderId.isEmpty()) {
                showToast("Invalid Order ID")
                return@setOnClickListener
            }

            btnSubmitOrder.isEnabled = false
            btnSubmitOrder.text = "Verifying..."

            lifecycleScope.launch {
                try {
                    val token = sessionManager.getAuthToken() ?: return@launch
                    val req = VerifyOrderRequest(currentOrderId)
                    val res = apiService.verifyOrder(token, req)
                    if (res.isSuccessful && res.body() != null) {
                        showToast("Plan Upgraded Successfully!")
                        sessionManager.savePlanExpiry(res.body()!!.newExpiry)
                        if (res.body()!!.deviceLimit != null) {
                            val activeCount = res.body()!!.activeSessionsCount ?: sessionManager.getActiveSessionsCount()
                            sessionManager.saveDeviceLimits(res.body()!!.deviceLimit!!, activeCount)
                        }
                        displayPlanInfo()
                        bottomSheetDialog.dismiss()
                    } else {
                        showToast("Verification Failed")
                        btnSubmitOrder.isEnabled = true
                        btnSubmitOrder.text = "Try Again"
                    }
                } catch (e: Exception) {
                    showToast("Network Error")
                    btnSubmitOrder.isEnabled = true
                    btnSubmitOrder.text = "Try Again"
                }
            }
        }

        bottomSheetDialog.show()
    }
}