package com.example.alsmsrecive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alsmsrecive.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PlanActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvPlanStatus: TextView
    private lateinit var tvExpiryDate: TextView
    private lateinit var btnContactAdmin: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)
        sessionManager = SessionManager(applicationContext)

        tvPlanStatus = findViewById(R.id.tvPlanStatus)
        tvExpiryDate = findViewById(R.id.tvExpiryDate)
        btnContactAdmin = findViewById(R.id.btnContactAdmin)
        btnBack = findViewById(R.id.btnBackPlan)

        displayPlanInfo()

        btnContactAdmin.setOnClickListener {
            // অ্যাডমিনের সাথে যোগাযোগের লিংক (আপনার টেলিগ্রাম লিংক দিন)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/alaminvai03"))
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun displayPlanInfo() {
        val expiryStr = sessionManager.getPlanExpiry()

        if (expiryStr != null) {
            try {
                // সার্ভার থেকে আসা ডেট ফরম্যাট করা
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(expiryStr)

                val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedDate = outputFormat.format(date!!)

                tvExpiryDate.text = "Expires on: $formattedDate"

                if (sessionManager.isPlanExpired()) {
                    tvPlanStatus.text = "Status: EXPIRED"
                    tvPlanStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                } else {
                    tvPlanStatus.text = "Status: ACTIVE ✅"
                    tvPlanStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
            } catch (e: Exception) {
                tvExpiryDate.text = "Expires on: $expiryStr"
            }
        } else {
            tvExpiryDate.text = "Expires on: N/A"
            tvPlanStatus.text = "Status: UNKNOWN"
        }
    }
}