package com.fft.kitchen.utils

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.fft.kitchen.LabelResponse
import com.fft.kitchen.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LabelAdapter(private val labels: LinkedHashMap<String, LabelResponse>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_NORMAL = 1
    private val VIEW_TYPE_DEFROSTED = 2
    private val VIEW_TYPE_HEADER = 3

    companion object {
        // Helper function to extract dates using regex
        fun extractDates(dates: List<String>): List<String> {
            val datePattern = Regex("""\b\d{2}/\d{2}/\d{2} \d{2}:\d{2}\b""")
            return dates.flatMap { datePattern.findAll(it).map { match -> match.value }.toList() }
        }

        // Check if the label is expired
        fun isExpired(dates: List<String>): Boolean {
            val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

            val extractedDates = extractDates(dates)
            val expiryDate = extractedDates.lastOrNull() ?: return false // Use the last date for expiry
            return try {
                val expiryTime = formatter.parse(expiryDate)?.time ?: return false
                val currentTime = System.currentTimeMillis()
                expiryTime < currentTime
            } catch (e: Exception) {
                Log.e("isExpired", "Error parsing date: $expiryDate", e)
                false
            }
        }

        // Check if the label is soon-to-expire (same day)
        fun isSoonToExpire(dates: List<String>): Boolean {
            val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            val extractedDates = extractDates(dates)
            val expiryDate = extractedDates.lastOrNull() ?: return false // Use the last date for expiry
            return try {
                val expiryTime = formatter.parse(expiryDate)?.time ?: return false
                val currentTime = System.currentTimeMillis()
                isSameDay(expiryTime, currentTime)
            } catch (e: Exception) {
                Log.e("isSoonToExpire", "Error parsing date: $expiryDate", e)
                false
            }
        }

        // Check if the label is defrosted
        fun isDefrosted(dates: List<String>): Boolean {
            val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            val extractedDates = extractDates(dates)
            if (extractedDates.size < 3) return false // Ensure there are at least 3 dates
            return try {
                val defrostTime = formatter.parse(extractedDates[0])?.time ?: 0
                val readyToPrepTime = formatter.parse(extractedDates[1])?.time ?: 0
                val currentTime = System.currentTimeMillis()

                currentTime in defrostTime..readyToPrepTime
            } catch (e: Exception) {
                Log.e("isDefrosted", "Error parsing defrost dates: $dates", e)
                false
            }
        }

        // Helper function to check if two timestamps fall on the same day
        private fun isSameDay(date1: Long, date2: Long): Boolean {
            val calendar1 = Calendar.getInstance().apply { timeInMillis = date1 }
            val calendar2 = Calendar.getInstance().apply { timeInMillis = date2 }
            return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                    calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
        }

        // Determine the background color based on the label's status
        fun getBackgroundColor(dates: List<String>): Int {
            return when {
                isExpired(dates) -> Color.parseColor("#F74F4F") // Red for expired
                isDefrosted(dates) -> Color.WHITE // White for defrosted
                isSoonToExpire(dates) -> Color.parseColor("#F7E64F") // Yellow for soon-to-expire
                else -> Color.parseColor("#4FC3F7") // Blue for valid
            }
        }
    }

    override fun getItemCount(): Int = labels.size

    // ViewHolder for Normal Labels
    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val labelName: TextView = itemView.findViewById(R.id.labelName)
        val labelBatch: TextView = itemView.findViewById(R.id.labelBatch)
        val empName: TextView = itemView.findViewById(R.id.empName)
        val labelPreppedTitle: TextView = itemView.findViewById(R.id.labelPreppedTitle)
        val labelPrepped: TextView = itemView.findViewById(R.id.labelPrepped)
        val labelUseByTitle: TextView = itemView.findViewById(R.id.labelUseByTitle)
        val labelUseBy: TextView = itemView.findViewById(R.id.labelUseBy)
        val labelExpiry: TextView = itemView.findViewById(R.id.labelExpiry)
    }

    inner class DefrostedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val labelName: TextView = itemView.findViewById(R.id.labelName)
        val labelBatch: TextView = itemView.findViewById(R.id.labelBatch)
        val empName: TextView = itemView.findViewById(R.id.empName)
        val labelDefrostTitle: TextView = itemView.findViewById(R.id.labelDefrostTitle)
        val labelDefrost: TextView = itemView.findViewById(R.id.labelDefrost)
        val labelReadyToPrepTitle: TextView = itemView.findViewById(R.id.labelReadyToPrepTitle)
        val labelReadyToPrep: TextView = itemView.findViewById(R.id.labelReadyToPrep)
        val labelUseByTitle: TextView = itemView.findViewById(R.id.labelUseByTitle)
        val labelUseBy: TextView = itemView.findViewById(R.id.labelUseBy)
        val labelExpiry: TextView = itemView.findViewById(R.id.labelExpiry)
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.sectionHeaderText)
    }

    override fun getItemViewType(position: Int): Int {
        val label = labels.values.toList()[position].parsed_data
        return when (label.label_type) {
            "Header" -> VIEW_TYPE_HEADER
            "Defrosted" -> VIEW_TYPE_DEFROSTED
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_section_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_DEFROSTED -> {
                val view = inflater.inflate(R.layout.item_label_defrost, parent, false)
                DefrostedViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_label, parent, false)
                NormalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = labels.values.toList()[position]

        // Handle headers
        if (holder is HeaderViewHolder) {
            holder.headerText.text = item.parsed_data.product_name
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 4)
            }
            return
        }

        val label = item.parsed_data
        val backgroundColor = getBackgroundColor(label.dates)
        holder.itemView.findViewById<CardView>(R.id.cardView).setCardBackgroundColor(backgroundColor)

        when (holder) {
            is NormalViewHolder -> {
                // Product name with RTE status
                val productNameWithRTE = if (!label.rte_status.isNullOrEmpty()) {
                    "${label.product_name} ${label.rte_status}"
                } else {
                    label.product_name
                }
                holder.labelName.text = productNameWithRTE
                
                // Batch number
                holder.labelBatch.text = "Batch No: ${label.batch_no ?: "N/A"}"
                
                // Employee name
                holder.empName.text = label.employee_name ?: "Unknown"
                
                // Format dates
                if (label.dates.isNotEmpty()) {
                    // Prepped date (first date)
                    val preppedDate = label.dates.firstOrNull() ?: ""
                    holder.labelPreppedTitle.text = "PREPPED"
                    holder.labelPrepped.text = preppedDate
                    
                    // Use by date (last date)
                    val useByDate = label.dates.lastOrNull() ?: ""
                    holder.labelUseByTitle.text = "USE BY"
                    holder.labelUseBy.text = "$useByDate E.O.D"
                    
                    // Day of week for expiry
                    val dayOfWeek = try {
                        val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                        val date = formatter.parse(useByDate)
                        val calendar = Calendar.getInstance().apply { time = date }
                        when (calendar.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.MONDAY -> "MONDAY"
                            Calendar.TUESDAY -> "TUESDAY"
                            Calendar.WEDNESDAY -> "WEDNESDAY"
                            Calendar.THURSDAY -> "THURSDAY"
                            Calendar.FRIDAY -> "FRIDAY"
                            Calendar.SATURDAY -> "SATURDAY"
                            Calendar.SUNDAY -> "SUNDAY"
                            else -> ""
                        }
                    } catch (e: Exception) {
                        "UNKNOWN"
                    }
                    holder.labelExpiry.text = dayOfWeek
                }
            }
            is DefrostedViewHolder -> {
                // Product name with RTE status
                val productNameWithRTE = if (!label.rte_status.isNullOrEmpty()) {
                    "${label.product_name} ${label.rte_status}"
                } else {
                    label.product_name
                }
                holder.labelName.text = productNameWithRTE
                
                // Batch number
                holder.labelBatch.text = "Batch: ${label.batch_no ?: "N/A"}"
                
                // Employee name
                holder.empName.text = "Prep by: ${label.employee_name ?: "Unknown"}"
                
                // Format dates
                val datesText = StringBuilder("Dates:\n")
                label.dates.forEachIndexed { index, date ->
                    when (index) {
                        0 -> datesText.append("Prep: $date\n")
                        label.dates.size - 1 -> datesText.append("Use by: $date")
                        else -> datesText.append("Check: $date\n")
                    }
                }
                holder.labelDefrostTitle.text = "DEFROSTED"
                holder.labelDefrost.text = datesText.toString()
                
                // Use by date (last date)
                val useByDate = label.dates.lastOrNull() ?: ""
                holder.labelReadyToPrepTitle.text = "READY TO PREP"
                holder.labelReadyToPrep.text = useByDate
                
                // Day of week for expiry
                val dayOfWeek = try {
                    val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    val date = formatter.parse(useByDate)
                    val calendar = Calendar.getInstance().apply { time = date }
                    when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "MONDAY"
                        Calendar.TUESDAY -> "TUESDAY"
                        Calendar.WEDNESDAY -> "WEDNESDAY"
                        Calendar.THURSDAY -> "THURSDAY"
                        Calendar.FRIDAY -> "FRIDAY"
                        Calendar.SATURDAY -> "SATURDAY"
                        Calendar.SUNDAY -> "SUNDAY"
                        else -> ""
                    }
                } catch (e: Exception) {
                    "UNKNOWN"
                }
                holder.labelExpiry.text = dayOfWeek
            }
        }
        
        holder.itemView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 8, 0, 8) // Reduced overall margins
        }
    }

    fun getLabelAtPosition(position: Int): LabelResponse? {
        return labels.values.toList().getOrNull(position)
    }
}

