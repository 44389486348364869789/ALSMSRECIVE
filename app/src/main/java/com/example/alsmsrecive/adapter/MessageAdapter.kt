package com.example.alsmsrecive.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alsmsrecive.R
import com.example.alsmsrecive.network.models.MessageResponse
import java.time.Instant

class MessageAdapter(
    private val messages: MutableList<MessageResponse>,
    private val isSelectable: Boolean = false
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<MessageResponse>()
    private var selectionListener: ((Int) -> Unit)? = null

    // Gmail Style Color List
    private val iconColors = listOf(
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#673AB7"), // Deep Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#03A9F4"), // Light Blue
        Color.parseColor("#009688"), // Teal
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#795548"), // Brown
        Color.parseColor("#607D8B")  // Blue Grey
    )

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.findViewById(R.id.message_item_container)
        val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        val sender: TextView = itemView.findViewById(R.id.tvSender)
        val message: TextView = itemView.findViewById(R.id.tvMessage)
        val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val checkBox: CheckBox = itemView.findViewById(R.id.chk_select)
        val unreadIndicator: View? = itemView.findViewById(R.id.viewUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]

        // ১. নাম এবং অ্যাপ টাইপ সেট করা
        val displayName = msg.sender ?: "Unknown"
        val displayType = if (msg.type.isNullOrEmpty()) "" else " (${msg.type})"

        // নাম এবং টাইপ একসাথে সেট করছি
        holder.sender.text = "$displayName$displayType"

        // !!! ফিক্স: নাম সব সময় বোল্ড থাকবে (User Requirement) !!!
        holder.sender.setTypeface(null, Typeface.BOLD)

        holder.message.text = msg.message

        // ২. আইকনের কালার সেট করা
        val senderName = msg.sender ?: "?"
        val colorIndex = kotlin.math.abs(senderName.hashCode()) % iconColors.size
        holder.iconContainer.background.setTintList(ColorStateList.valueOf(iconColors[colorIndex]))
        holder.imgIcon.setColorFilter(Color.WHITE)

        // ৩. স্মার্ট সময়
        try {
            val timeMillis = Instant.parse(msg.timestamp).toEpochMilli()
            val niceTime = DateUtils.getRelativeTimeSpanString(
                timeMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            holder.timestamp.text = niceTime
        } catch (e: Exception) {
            holder.timestamp.text = "Just now"
        }

        // ৪. Unread / Read লজিক
        // নোট: সেন্ডার নেম উপরেই বোল্ড করা হয়েছে, এখানে শুধু মেসেজ বডি চেঞ্জ হবে
        val isRead = true // (মডেলে isRead থাকলে সেটা দিন)

        if (!isRead) {
            // পড়া হয়নি: মেসেজ বডি বোল্ড এবং কালো
            holder.message.setTypeface(null, Typeface.BOLD)
            holder.message.setTextColor(Color.BLACK)
            holder.unreadIndicator?.visibility = View.VISIBLE
        } else {
            // পড়া হয়েছে: মেসেজ বডি নরমাল এবং ধূসর
            holder.message.setTypeface(null, Typeface.NORMAL)
            holder.message.setTextColor(Color.parseColor("#757575"))
            holder.unreadIndicator?.visibility = View.GONE
        }

        // ৫. সিলেকশন মোড লজিক
        if (isSelectable) {
            holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            holder.checkBox.isChecked = selectedItems.contains(msg)

            if (selectedItems.contains(msg)) {
                holder.container.setBackgroundColor(Color.parseColor("#E3F2FD"))
            } else {
                holder.container.setBackgroundColor(Color.TRANSPARENT)
            }

            holder.container.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(holder, msg)
                }
                true
            }

            holder.container.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(holder, msg)
                }
            }

            holder.checkBox.setOnClickListener {
                toggleSelection(holder, msg)
            }
        }
    }

    private fun toggleSelection(holder: MessageViewHolder, msg: MessageResponse) {
        if (selectedItems.contains(msg)) {
            selectedItems.remove(msg)
        } else {
            selectedItems.add(msg)
        }
        notifyItemChanged(holder.adapterPosition)
        selectionListener?.invoke(selectedItems.size)
    }

    fun setOnSelectionListener(listener: (Int) -> Unit) {
        this.selectionListener = listener
    }

    fun getSelectedItems(): List<String> {
        return selectedItems.map { it.id }
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedItems.clear()
        selectionListener?.invoke(0)
        notifyDataSetChanged()
    }

    fun isAllSelected(): Boolean {
        return selectedItems.size == messages.size && messages.isNotEmpty()
    }

    fun selectAll() {
        isSelectionMode = true
        selectedItems.clear()
        selectedItems.addAll(messages)
        notifyDataSetChanged()
        selectionListener?.invoke(selectedItems.size)
    }
}