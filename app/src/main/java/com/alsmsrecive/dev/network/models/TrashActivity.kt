// TrashActivity.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView // <-- ImageButton এর বদলে TextView ইম্পোর্ট করুন
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alsmsrecive.dev.adapter.MessageAdapter
import com.alsmsrecive.dev.network.ApiClient
import com.alsmsrecive.dev.network.DeleteRequest
import com.alsmsrecive.dev.network.RestoreRequest
import com.alsmsrecive.dev.network.models.MessageResponse
import com.alsmsrecive.dev.utils.SessionManager
import kotlinx.coroutines.launch

class TrashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance

    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomActionBar: LinearLayout
    private lateinit var btnRestore: Button
    private lateinit var btnDeleteForever: Button

    // --- !!! নতুন ভেরিয়েবল !!! ---
    private lateinit var btnSelectAll: TextView

    private lateinit var messageAdapter: MessageAdapter
    private var messageList = mutableListOf<MessageResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)
        sessionManager = SessionManager(applicationContext)

        // UI লিঙ্ক করা
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBarTrash)
        recyclerView = findViewById(R.id.recyclerViewTrash)
        bottomActionBar = findViewById(R.id.bottom_action_bar)
        btnRestore = findViewById(R.id.btnRestore)
        btnDeleteForever = findViewById(R.id.btnDeleteForever)

        // --- !!! নতুন বাটন লিঙ্ক করুন !!! ---
        btnSelectAll = findViewById(R.id.btnSelectAll)

        // বাটন সেটআপ
        btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadDeletedMessages()

        btnRestore.setOnClickListener { handleRestore() }

        // --- !!! "Delete Forever" বাটন ক্লিক লিসেনার পরিবর্তন করা হয়েছে !!! ---
        btnDeleteForever.setOnClickListener { confirmDeleteForever() } // এটি প্রথম কনফার্মেশন দেখাবে

        // --- !!! "Select All" বাটন ক্লিক লিসেনার !!! ---
        btnSelectAll.setOnClickListener {
            handleSelectAll()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, isSelectable = true)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // --- !!! সিলেকশন লিসেনার পরিবর্তন করা হয়েছে !!! ---
        messageAdapter.setOnSelectionListener { count ->
            if (count > 0) {
                bottomActionBar.visibility = View.VISIBLE
                btnRestore.text = "Restore ($count)"
                btnDeleteForever.text = "Delete Forever ($count)"

                // সব সিলেক্টেড হলে লেখা পরিবর্তন করুন
                if (messageAdapter.isAllSelected()) {
                    btnSelectAll.text = "Unselect All"
                } else {
                    btnSelectAll.text = "Select All"
                }

            } else {
                bottomActionBar.visibility = View.GONE
                btnSelectAll.text = "Select All" // কাউন্ট ০ হলে লেখা ঠিক করুন
            }
        }
    }

    // --- !!! এই নতুন ফাংশনটি যোগ করা হয়েছে !!! ---
    private fun handleSelectAll() {
        if (messageList.isEmpty()) return // লিস্ট খালি থাকলে কিছু করবেনা

        if (messageAdapter.isAllSelected()) {
            messageAdapter.clearSelection() // সব সিলেক্টেড থাকলে, সব আন-সিলেক্ট করুন
        } else {
            messageAdapter.selectAll() // না থাকলে, সব সিলেক্ট করুন
        }
    }

    private fun loadDeletedMessages() {
        showLoading(true)
        val token = sessionManager.getAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getDeletedMessages(token)
                if (response.isSuccessful) {
                    messageList.clear()
                    messageList.addAll(response.body() ?: emptyList())
                    messageAdapter.notifyDataSetChanged()

                    // --- !!! লোড হওয়ার পর "Select All" বাটনের লেখা ঠিক করুন !!! ---
                    messageAdapter.clearSelection() // লোড করার পর সিলেকশন রিসেট করুন

                    if (messageList.isEmpty()) {
                        showToast("Trash is empty.")
                        btnSelectAll.visibility = View.GONE // ট্র্যাশ খালি থাকলে বাটন দেখানোর দরকার নেই
                    } else {
                        btnSelectAll.visibility = View.VISIBLE
                    }
                } else {
                    showToast("Failed to load trash")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleRestore() {
        val selectedIds = messageAdapter.getSelectedItems()
        if (selectedIds.isEmpty()) {
            showToast("No items selected")
            return
        }

        showLoading(true)
        val token = sessionManager.getAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.restoreMessages(token, RestoreRequest(selectedIds))
                if (response.isSuccessful) {
                    showToast("Messages restored!")
                    messageAdapter.clearSelection()
                    loadDeletedMessages()
                } else {
                    showToast("Failed to restore messages")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // --- !!! ডাবল কনফার্মেশন লজিক যোগ করা হয়েছে !!! ---

    // ধাপ ১: প্রথম কনফার্মেশন
    private fun confirmDeleteForever() {
        val selectedCount = messageAdapter.getSelectedItems().size
        if (selectedCount == 0) {
            showToast("No items selected")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete $selectedCount Items?")
            .setMessage("Are you sure you want to permanently delete these items? This action cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ ->
                showSecondConfirmation() // দ্বিতীয় কনফার্মেশন দেখান
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ধাপ ২: দ্বিতীয় কনফার্মেশন (নতুন ফাংশন)
    private fun showSecondConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Final Warning")
            .setMessage("This is the FINAL confirmation. The selected items will be permanently erased. Are you absolutely sure?")
            .setPositiveButton("Yes, I Am Sure, Delete!") { _, _ ->
                handleDeleteForever() // এখন ডিলিট করুন
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ধাপ ৩: আসল ডিলিট ফাংশন (আগের মতোই)
    private fun handleDeleteForever() {
        val selectedIds = messageAdapter.getSelectedItems()
        // কাউন্ট চেক করার দরকার নেই, কারণ confirmDeleteForever-এ করা হয়েছে

        showLoading(true)
        val token = sessionManager.getAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.deleteMessagesByIds(token, DeleteRequest(selectedIds))
                if (response.isSuccessful) {
                    showToast("Messages permanently deleted")
                    messageAdapter.clearSelection()
                    loadDeletedMessages()
                } else {
                    showToast("Failed to delete messages")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    // -------------------------------------------

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}