package com.fft.kitchen

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fft.kitchen.utils.LabelAdapter
import com.fft.kitchen.utils.RecyclerViewTouchListener
import com.fft.kitchen.data.RetrofitClient
import com.fft.kitchen.data.ApiService
import com.fft.kitchen.data.FieldType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import com.fft.kitchen.printer.LabelPrinterHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.fft.kitchen.printer.PrinterSettings

class LabelManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LabelAdapter
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_manager)

        // Initialize RetrofitClient and API service
        RetrofitClient.initialize(this)
        apiService = RetrofitClient.api

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.savedLabelsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load and display labels from API
        loadSavedLabels()

        // Handle long press for deleting a label
        recyclerView.addOnItemTouchListener(
            RecyclerViewTouchListener(this, recyclerView,
                object : RecyclerViewTouchListener.ClickListener {
                    override fun onClick(view: View, position: Int) {
                        // Print the label when clicked
                        val label = adapter.getLabelAtPosition(position)
                        if (label != null && !label.parsed_data.label_type.equals("Header", ignoreCase = true)) {
                            printLabel(label)
                        }
                    }

                    override fun onLongClick(view: View, position: Int) {
                        val label = adapter.getLabelAtPosition(position)
                        if (label != null && !label.parsed_data.label_type.equals("Header", ignoreCase = true)) {
                            showDeleteConfirmationDialog(label)
                        }
                    }
                })
        )
    }

    private fun showDeleteConfirmationDialog(label: LabelResponse) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Label")
            .setMessage("Are you sure you want to delete the label for ${label.parsed_data.product_name}?")
            .setPositiveButton("Delete") { _, _ -> deleteLabel(label) }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun deleteLabel(label: LabelResponse) {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Deleting Label")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        apiService.deleteLabel(label.label_id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                loadingDialog.dismiss()
                
                if (response.isSuccessful) {
                    Toast.makeText(this@LabelManagerActivity, 
                        "Label deleted successfully", 
                        Toast.LENGTH_SHORT).show()
                    loadSavedLabels() // Refresh the list of labels
                } else {
                    Toast.makeText(this@LabelManagerActivity, 
                        "Failed to delete label: ${response.code()}", 
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                loadingDialog.dismiss()
                Toast.makeText(this@LabelManagerActivity, 
                    "Error deleting label: ${t.message}", 
                    Toast.LENGTH_SHORT).show()
                Log.e("LabelManagerActivity", "Error deleting label", t)
            }
        })
    }

    private fun printLabel(label: LabelResponse) {
        // Show printing dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Printing Label")
            .setMessage("Preparing label for printing...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        Thread {
            try {
                // Initialize the printer helper
                val printerHelper = LabelPrinterHelper(this)
                printerHelper.bindService(object : LabelPrinterHelper.PrinterCallback {
                    override fun onConnected() {
                        // Format the label with proper layout
                        val formattedLabel = formatLabelContent(label)
                        
                        // Print using local AIDL service
                        printerHelper.printLabel(formattedLabel)
                        
                        // Unbind when done
                        printerHelper.unbindService()
                        
                        runOnUiThread {
                            loadingDialog.dismiss()
                            Toast.makeText(this@LabelManagerActivity, 
                                "Label printed successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onDisconnected() {
                        runOnUiThread {
                            loadingDialog.dismiss()
                            Toast.makeText(this@LabelManagerActivity, 
                                "Printer disconnected", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onError(message: String) {
                        runOnUiThread {
                            loadingDialog.dismiss()
                            Toast.makeText(this@LabelManagerActivity, 
                                "Printer error: $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onCalibrationComplete() {
                        // Not used for this operation
                    }
                })
            } catch (e: Exception) {
                Log.e("printLabel", "Error printing label: ${e.message}", e)
                runOnUiThread {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    /**
     * Format label content with proper layout positioning
     * Using escape sequences and spacing to position content correctly
     */
    private fun formatLabelContent(label: LabelResponse): String {
        val layout = PrinterSettings.getInstance(this).getLabelLayout()
        val sb = StringBuilder()

        // Process each element in the layout
        layout.elements.forEach { element ->
            // Get the actual value based on field type
            val value = when (element.fieldType) {
                FieldType.PRODUCT_NAME -> label.parsed_data.product_name ?: "Unknown Product"
                FieldType.BATCH_NO -> "${element.text}${label.parsed_data.batch_no ?: "N/A"}"
                FieldType.EMPLOYEE_NAME -> "${element.text}${label.parsed_data.employee_name ?: "Unknown Employee"}"
                FieldType.PREP_DATE -> "${element.text}${label.parsed_data.dates?.getOrNull(0) ?: "N/A"}"
                FieldType.USE_BY_DATE -> "${element.text}${label.parsed_data.dates?.lastOrNull() ?: "N/A"}"
                FieldType.CHECK_DATE -> "${element.text}${label.parsed_data.dates?.getOrNull(1) ?: "N/A"}"
                FieldType.CUSTOM_TEXT -> element.text
                else -> element.text
            }

            // Add positioning commands
            sb.append("\u001B*${element.x.toInt()},${element.y.toInt()}")  // Position cursor
            sb.append("\u001BF${element.fontSize.toInt()}")  // Set font size
            if (element.isBold) sb.append("\u001BE")  // Set bold
            sb.append(value)
            if (element.isBold) sb.append("\u001BF")  // Reset bold
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun loadSavedLabels() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Loading Labels")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch labels from API
        apiService.getLabels().enqueue(object : Callback<List<LabelResponse>> {
            override fun onResponse(call: Call<List<LabelResponse>>, response: Response<List<LabelResponse>>) {
                loadingDialog.dismiss()
                
                if (response.isSuccessful) {
                    val labels = response.body() ?: emptyList()
                    Log.d("LabelManagerActivity", "Fetched ${labels.size} labels from API")
                    
                    // Convert list to map with label_id as key
                    val labelsMap = labels.associateBy { it.label_id }
                    
                    // Categorize and flatten labels
                    val categorizedLabels = categorizeLabelsUsingAdapter(labels)
                    val flattenedMap = flattenCategorizedLabels(categorizedLabels)
                    
                    // Update adapter with new data
                    adapter = LabelAdapter(flattenedMap)
                    recyclerView.adapter = adapter
                    
                    // Clear existing items
                    clearRecyclerView()
                    
                    // Invalidate RecyclerView
                    recyclerView.invalidate()
                } else {
                    Toast.makeText(this@LabelManagerActivity, 
                        "Failed to load labels: ${response.code()}", 
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LabelResponse>>, t: Throwable) {
                loadingDialog.dismiss()
                Toast.makeText(this@LabelManagerActivity, 
                    "Error loading labels: ${t.message}", 
                    Toast.LENGTH_SHORT).show()
                Log.e("LabelManagerActivity", "Error fetching labels", t)
            }
        })
    }

    private fun clearRecyclerView() {
        val itemCount = adapter.itemCount
        if (itemCount > 0) {
            adapter.notifyItemRangeRemoved(0, itemCount)
        }
    }

    private fun categorizeLabelsUsingAdapter(labels: List<LabelResponse>): Map<String, List<LabelResponse>> {
        val expired = mutableListOf<LabelResponse>()
        val soonToExpire = mutableListOf<LabelResponse>()
        val valid = mutableListOf<LabelResponse>()

        labels.forEach { label ->
            val dates = label.parsed_data.dates
            when {
                LabelAdapter.isExpired(dates) -> {
                    expired.add(label)
                    Log.d("categorizeLabels", "Label expired: ${label.label_id}")
                }
                LabelAdapter.isSoonToExpire(dates) -> {
                    soonToExpire.add(label)
                    Log.d("categorizeLabels", "Label soon to expire: ${label.label_id}")
                }
                else -> {
                    valid.add(label)
                    Log.d("categorizeLabels", "Label valid: ${label.label_id}")
                }
            }
        }

        Log.d("categorizeLabels", "Expired: ${expired.size}, Soon-to-Expire: ${soonToExpire.size}, Valid: ${valid.size}")
        return mapOf(
            "Critical" to expired,
            "Soon To Expire" to soonToExpire,
            "Valid" to valid
        )
    }

    private fun flattenCategorizedLabels(categorizedLabels: Map<String, List<LabelResponse>>): LinkedHashMap<String, LabelResponse> {
        val flattenedMap = LinkedHashMap<String, LabelResponse>()

        categorizedLabels.forEach { (category, labels) ->
            // Create a proper header label
            val headerLabel = LabelResponse(
                label_id = "header_$category",
                parsed_data = ParsedData(
                    product_name = category,
                    dates = emptyList(),
                    employee_name = "",
                    batch_no = "",
                    label_type = "Header",
                    rte_status = "",
                    expiry_day = "",
                    defrost_date = null,
                    ready_to_prep_date = null,
                    use_by_date = null
                ),
                raw_text = category
            )
            flattenedMap[headerLabel.label_id] = headerLabel
            
            // Add the actual labels
            labels.forEach { label ->
                flattenedMap[label.label_id] = label
            }
        }

        Log.d("LabelManagerActivity", "Flattened Map Keys: ${flattenedMap.keys}")
        return flattenedMap
    }
}