package com.example.alsmsrecive

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText // <-- নতুন ইম্পোর্ট
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener // <-- নতুন ইম্পোর্ট
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alsmsrecive.adapter.AppAdapter
import com.example.alsmsrecive.adapter.AppInfo
import com.example.alsmsrecive.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    // --- !!! নতুন ভেরিয়েবল যোগ করা হয়েছে !!! ---
    private lateinit var etSearchApp: EditText
    private var fullAppList = mutableListOf<AppInfo>() // সব অ্যাপের মূল তালিকা
    private var displayedAppList = mutableListOf<AppInfo>() // ফিল্টার করা তালিকা (যা ইউজার দেখবে)
    // ------------------------------------------

    private lateinit var appAdapter: AppAdapter
    private var selectedPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        sessionManager = SessionManager(applicationContext)

        // UI লিঙ্ক করা
        recyclerViewApps = findViewById(R.id.recyclerViewApps)
        progressBar = findViewById(R.id.progressBarAppList)
        btnSave = findViewById(R.id.btnSaveAppSelection)
        btnBack = findViewById(R.id.btnAppSelectBack)
        etSearchApp = findViewById(R.id.etSearchApp) // <-- সার্চ বার লিঙ্ক করা

        setupRecyclerView()

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveSelection() }

        // --- !!! সার্চ লিসেনার যোগ করা হয়েছে !!! ---
        etSearchApp.addTextChangedListener { text ->
            filterAndSortList(text.toString())
        }

        loadApps()
    }

    // --- !!! setupRecyclerView() পরিবর্তন করা হয়েছে !!! ---
    private fun setupRecyclerView() {
        selectedPackages = sessionManager.getSelectedAppPackages().toMutableSet()

        // অ্যাডাপ্টারকে 'displayedAppList' দিন, 'fullAppList' নয়
        appAdapter = AppAdapter(displayedAppList, selectedPackages)

        // --- !!! এই লিসেনারটি যোগ করা হয়েছে (সমস্যা ১ ও ২ এর সমাধান) !!! ---
        appAdapter.setOnItemClickedListener {
            // যখনই কোনো আইটেম ক্লিক হবে, সার্চ কোয়েরি ঠিক রেখে তালিকাটি পুনরায় সর্ট করুন
            filterAndSortList(etSearchApp.text.toString())
        }

        recyclerViewApps.adapter = appAdapter
        recyclerViewApps.layoutManager = LinearLayoutManager(this)
    }

    // --- !!! loadApps() পরিবর্তন করা হয়েছে !!! ---
    private fun loadApps() {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            // নতুন 'fullAppList'-এ ডেটা লোড করুন
            val tempFullList = mutableListOf<AppInfo>()

            for (appInfo in installedApps) {
                if (appInfo.packageName == packageName) continue

                if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                    val appName = appInfo.loadLabel(pm).toString()
                    val packageName = appInfo.packageName
                    val icon = appInfo.loadIcon(pm)
                    tempFullList.add(AppInfo(appName, packageName, icon))
                }
            }

            withContext(Dispatchers.Main) {
                fullAppList.clear()
                fullAppList.addAll(tempFullList)

                // লোড হওয়ার পর তালিকাটি ফিল্টার ও সর্ট করুন
                filterAndSortList()

                showLoading(false)
            }
        }
    }

    // --- !!! এই নতুন ফাংশনটি সব কাজ করবে (সর্ট এবং সার্চ) !!! ---
    private fun filterAndSortList(query: String = "") {
        // ১. সার্চ (ফিল্টার)
        val filteredList = if (query.isEmpty()) {
            fullAppList // সার্চ বার খালি থাকলে সব অ্যাপ দেখান
        } else {
            fullAppList.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }

        // ২. সর্ট (সিলেক্টেড অ্যাপ উপরে)
        val selectedList = filteredList
            .filter { selectedPackages.contains(it.packageName) }
            .sortedBy { it.appName.lowercase() }

        val unselectedList = filteredList
            .filter { !selectedPackages.contains(it.packageName) }
            .sortedBy { it.appName.lowercase() }

        // ৩. অ্যাডাপ্টারের তালিকা আপডেট
        displayedAppList.clear()
        displayedAppList.addAll(selectedList)
        displayedAppList.addAll(unselectedList)
        appAdapter.notifyDataSetChanged()
    }


    private fun saveSelection() {
        val finalSelectedPackages = appAdapter.getSelectedPackages()
        sessionManager.saveSelectedAppPackages(finalSelectedPackages)
        Toast.makeText(this, "Selection saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerViewApps.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}