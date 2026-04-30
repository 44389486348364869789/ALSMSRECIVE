// adapter/AppAdapter.kt (সম্পূর্ণ সংশোধিত)
package com.example.alsmsrecive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alsmsrecive.R

class AppAdapter(
    private val appList: List<AppInfo>,
    private val selectedPackages: MutableSet<String>
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    // --- !!! এই নতুন লিসেনারটি যোগ করা হয়েছে !!! ---
    private var onItemClicked: (() -> Unit)? = null

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        val appName: TextView = itemView.findViewById(R.id.tvAppName)
        val checkBox: CheckBox = itemView.findViewById(R.id.chkAppSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return AppViewHolder(view)
    }

    override fun getItemCount() = appList.size

    // --- !!! onBindViewHolder পরিবর্তন করা হয়েছে (গুরুত্বপূর্ণ) !!! ---
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = appList[position]
        holder.appName.text = appInfo.appName
        holder.appIcon.setImageDrawable(appInfo.icon)

        // রিসাইক্লিং বাগ এড়ানোর জন্য লিসেনার null করা হলো
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.itemView.setOnClickListener(null)

        // বর্তমান অবস্থা সেট করুন
        holder.checkBox.isChecked = selectedPackages.contains(appInfo.packageName)

        // নতুন ক্লিক লিসেনার (এটিই সঠিক উপায়)
        val clickListener = View.OnClickListener {
            val isChecked = !holder.checkBox.isChecked // ক্লিক করলে নতুন অবস্থা কী হবে?
            holder.checkBox.isChecked = isChecked // চেকবক্সকে সেই অবস্থায় সেট করুন

            // সেটের (Set) মধ্যে ডেটা আপডেট করুন
            if (isChecked) {
                selectedPackages.add(appInfo.packageName)
            } else {
                selectedPackages.remove(appInfo.packageName)
            }
            // অ্যাক্টিভিটিকে বলুন যে তালিকাটি আবার সর্ট (Sort) করতে হবে
            onItemClicked?.invoke()
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.checkBox.setOnClickListener(clickListener)
    }

    fun getSelectedPackages(): Set<String> {
        return selectedPackages
    }

    // --- !!! এই নতুন ফাংশনটি যোগ করা হয়েছে !!! ---
    fun setOnItemClickedListener(listener: () -> Unit) {
        onItemClicked = listener
    }
}