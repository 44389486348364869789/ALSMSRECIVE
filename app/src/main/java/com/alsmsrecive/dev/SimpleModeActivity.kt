// SimpleModeActivity.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alsmsrecive.dev.adapter.WorldClock
import com.alsmsrecive.dev.adapter.WorldClockAdapter
import com.alsmsrecive.dev.utils.SessionManager
import com.alsmsrecive.dev.utils.UiMode

class SimpleModeActivity : AppCompatActivity() {

    private lateinit var rlTitleBar: RelativeLayout
    private lateinit var clockRecyclerView: RecyclerView
    private lateinit var clockAdapter: WorldClockAdapter

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_mode)

        sessionManager = SessionManager(applicationContext)
        // সেশন ম্যানেজারকে বলুন যে আমরা এখন সিম্পল মোডে আছি
        // (এটি onCreate-তে সেট করা জরুরি)
        sessionManager.saveUiMode(UiMode.SIMPLE)

        // --- UI লিঙ্ক করা ---
        rlTitleBar = findViewById(R.id.rlTitleBar)
        clockRecyclerView = findViewById(R.id.worldClockRecyclerView)

        // --- RecyclerView সেটআপ ---
        setupWorldClock()

        // --- লং ক্লিক লিসেনার সেটআপ ---
        setupLongPressListener()
    }

    private fun setupLongPressListener() {
        rlTitleBar.setOnLongClickListener {
            Toast.makeText(this, "Opening Security Hub...", Toast.LENGTH_SHORT).show()
            goToMainActivity()
            true
        }
    }

    private fun setupWorldClock() {
        val clockList = listOf(
            WorldClock("Dhaka", "Asia/Dhaka"),
            WorldClock("London", "Europe/London"),
            WorldClock("New York", "America/New_York"),
            WorldClock("Tokyo", "Asia/Tokyo"),
            WorldClock("Sydney", "Australia/Sydney"),
            WorldClock("Dubai", "Asia/Dubai")
        )

        clockAdapter = WorldClockAdapter(clockList)
        clockRecyclerView.adapter = clockAdapter
        clockRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    // --- হেল্পার ফাংশন (সংশোধিত) ---
    private fun goToMainActivity() {
        // সিকিউরিটি মোডে যাওয়ার আগে মোড সেভ করুন
        sessionManager.saveUiMode(UiMode.SECURITY)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // !!! এই অ্যাক্টিভিটি (SimpleModeActivity) বন্ধ করুন !!!
        finish()
    }
}