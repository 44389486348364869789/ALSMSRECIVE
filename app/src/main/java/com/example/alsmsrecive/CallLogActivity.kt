// CallLogActivity.kt (নতুন ফাইল)
package com.example.alsmsrecive

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alsmsrecive.adapter.CallLogAdapter
import com.example.alsmsrecive.network.ApiClient
import com.example.alsmsrecive.network.CallLogResponse
import com.example.alsmsrecive.utils.SessionManager
import kotlinx.coroutines.launch

class CallLogActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance

    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView

    private lateinit var callLogAdapter: CallLogAdapter
    private var callLogList = mutableListOf<CallLogResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_log)
        sessionManager = SessionManager(applicationContext)

        // UI লিঙ্ক করা
        btnBack = findViewById(R.id.btnCallLogBack)
        progressBar = findViewById(R.id.progressBarCallLog)
        recyclerView = findViewById(R.id.recyclerViewCallLog)

        btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadCallLogs()
    }

    private fun setupRecyclerView() {
        callLogAdapter = CallLogAdapter(callLogList)
        recyclerView.adapter = callLogAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadCallLogs() {
        showLoading(true)
        val token = sessionManager.getAuthToken() ?: return

        lifecycleScope.launch {
            try {
                // নতুন API কল করা হচ্ছে
                val response = apiService.getCallLogs(token)
                if (response.isSuccessful) {
                    callLogList.clear()
                    callLogList.addAll(response.body() ?: emptyList())
                    callLogAdapter.notifyDataSetChanged()
                    if (callLogList.isEmpty()) {
                        showToast("No call logs found on server.")
                    }
                } else {
                    showToast("Failed to load call logs")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}