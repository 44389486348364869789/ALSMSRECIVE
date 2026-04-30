// adapter/WorldClockAdapter.kt
package com.alsmsrecive.dev.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextClock
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alsmsrecive.dev.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

// একটি সহজ ডেটা ক্লাস (City Name এবং Time Zone ID)
data class WorldClock(val city: String, val zoneId: String)

class WorldClockAdapter(private val timeZones: List<WorldClock>) :
    RecyclerView.Adapter<WorldClockAdapter.ClockViewHolder>() {

    class ClockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val textClock: TextClock = itemView.findViewById(R.id.textClock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_world_clock, parent, false)
        return ClockViewHolder(view)
    }

    override fun getItemCount() = timeZones.size

    override fun onBindViewHolder(holder: ClockViewHolder, position: Int) {
        val clock = timeZones[position]

        // টাইমজোন সেট করা (TextClock নিজে থেকেই সময় আপডেট করবে)
        holder.textClock.timeZone = clock.zoneId
        holder.tvCityName.text = clock.city

        // তারিখ এবং সময় পার্থক্য দেখানোর জন্য
        try {
            val zone = ZoneId.of(clock.zoneId)
            val zonedDateTime = ZonedDateTime.now(zone)
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM")
            holder.tvDate.text = zonedDateTime.format(formatter)
        } catch (e: Exception) {
            holder.tvDate.text = "Invalid Date"
        }
    }
}