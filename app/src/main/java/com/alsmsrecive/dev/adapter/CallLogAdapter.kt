// adapter/CallLogAdapter.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alsmsrecive.dev.R
import com.alsmsrecive.dev.network.CallLogResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CallLogAdapter(
    private val callLogs: MutableList<CallLogResponse>
) : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val callTypeIcon: ImageView = itemView.findViewById(R.id.imgCallType)
        val callNumber: TextView = itemView.findViewById(R.id.tvCallNumber)
        val callDate: TextView = itemView.findViewById(R.id.tvCallDate)
        val callDuration: TextView = itemView.findViewById(R.id.tvCallDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    override fun getItemCount() = callLogs.size

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val log = callLogs[position]

        holder.callNumber.text = log.number
        holder.callDate.text = formatTimestamp(log.date)
        holder.callDuration.text = formatDuration(log.duration)

        // --- !!! আইকন কোড আপডেট করা হয়েছে !!! ---
        when (log.type) {
            "INCOMING" -> holder.callTypeIcon.setImageResource(R.drawable.ic_call_received)
            "OUTGOING" -> holder.callTypeIcon.setImageResource(R.drawable.ic_call_made)
            "MISSED" -> holder.callTypeIcon.setImageResource(R.drawable.ic_call_missed)
            "REJECTED" -> holder.callTypeIcon.setImageResource(R.drawable.ic_call_missed) // Rejected-কেও missed (লাল) দেখানো হলো
            else -> holder.callTypeIcon.setImageResource(R.drawable.ic_call) // Unknown (ধূসর)
        }
    }

    // তারিখ ফরম্যাট হেল্পার
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val instant = Instant.parse(timestamp)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM")
            dateTime.format(formatter)
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    // সময় ফরম্যাট হেল্পার (সংশোধিত)
    private fun formatDuration(secondsStr: String): String {
        return try {
            val seconds = secondsStr.toLong()
            if (seconds == 0L) return "0s" // 0 সেকেন্ড হলে 바로 রিটার্ন

            val minutes = seconds / 60
            val remainingSeconds = seconds % 60

            if (minutes > 0 && remainingSeconds > 0) {
                "${minutes}m ${remainingSeconds}s"
            } else if (minutes > 0) {
                "${minutes}m"
            } else {
                "${remainingSeconds}s"
            }
        } catch (e: Exception) {
            "0s"
        }
    }
}