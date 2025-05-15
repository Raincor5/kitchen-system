package com.fft.kitchen.services

import android.content.Context
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import com.fft.kitchen.LabelResponse
import com.fft.kitchen.printer.LabelPrinterHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class LabelPrintingService(private val context: Context) {
    private val printerHelper = LabelPrinterHelper(context)
    private val printQueue = ConcurrentLinkedQueue<LabelResponse>()
    private var isPrinting = false
    private var retryCount = 0
    private val maxRetries = 3

    init {
        printerHelper.bindService(object : LabelPrinterHelper.PrinterCallback {
            override fun onConnected() {
                Log.d("PrinterService", "Printer connected")
                startPrintingQueue()
            }

            override fun onDisconnected() {
                Log.d("PrinterService", "Printer disconnected")
                isPrinting = false
                // Try to reconnect
                if (retryCount < maxRetries) {
                    retryCount++
                    Log.d("PrinterService", "Attempting to reconnect (attempt $retryCount)")
                    printerHelper.bindService(this)
                } else {
                    Log.e("PrinterService", "Failed to reconnect after $maxRetries attempts")
                    Toast.makeText(context, "Printer connection lost. Please check the printer.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(message: String) {
                Log.e("PrinterService", "Printer error: $message")
                isPrinting = false
                Toast.makeText(context, "Printer error: $message", Toast.LENGTH_SHORT).show()
                // Handle specific error cases
                when {
                    message.contains("paper", ignoreCase = true) -> {
                        Toast.makeText(context, "Please check paper supply", Toast.LENGTH_LONG).show()
                    }
                    message.contains("connection", ignoreCase = true) -> {
                        // Try to reconnect
                        if (retryCount < maxRetries) {
                            retryCount++
                            printerHelper.bindService(this)
                        }
                    }
                    else -> {
                        // Generic error handling
                        Toast.makeText(context, "Printer error: Please check the device", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onCalibrationComplete() {
                Log.d("PrinterService", "Printer calibration completed")
                // Reset printer settings to default after calibration
                try {
                    printerHelper.printerInit()
                    Toast.makeText(context, "Printer calibration completed", Toast.LENGTH_SHORT).show()
                    // Resume printing if there are items in the queue
                    if (!isPrinting && !printQueue.isEmpty()) {
                        startPrintingQueue()
                    }
                } catch (e: RemoteException) {
                    Log.e("PrinterService", "Error initializing printer after calibration", e)
                    onError("Failed to initialize printer after calibration")
                }
            }
        })
    }

    fun addToPrintQueue(label: LabelResponse?) {
        if (label != null) {
            printQueue.offer(label)
            if (!isPrinting) {
                startPrintingQueue()
            }
        }
    }

    fun addToPrintQueue(labels: List<LabelResponse>) {
        printQueue.addAll(labels)
        if (!isPrinting) {
            startPrintingQueue()
        }
    }

    private fun startPrintingQueue() {
        if (isPrinting || printQueue.isEmpty()) return

        isPrinting = true
        CoroutineScope(Dispatchers.IO).launch {
            while (printQueue.isNotEmpty() && isPrinting) {
                val label = printQueue.poll()
                try {
                    if (label != null) {
                        printLabel(label)
                    }
                } catch (e: RemoteException) {
                    Log.e("PrinterService", "Error printing label", e)
                    // Handle printing error
                    handlePrintError(e)
                    // If there was an error, pause printing
                    isPrinting = false
                    break
                }
            }
            isPrinting = false
        }
    }

    private fun handlePrintError(error: RemoteException) {
        CoroutineScope(Dispatchers.Main).launch {
            val errorMessage = when {
                error.message?.contains("connection", ignoreCase = true) == true -> {
                    "Lost connection to printer. Please check the connection."
                }
                error.message?.contains("paper", ignoreCase = true) == true -> {
                    "Paper error. Please check paper supply."
                }
                error.message?.contains("busy", ignoreCase = true) == true -> {
                    "Printer is busy. Will retry automatically."
                }
                else -> {
                    "Printing error: ${error.message}"
                }
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

            // If printer is just busy, retry after a delay
            if (error.message?.contains("busy", ignoreCase = true) == true) {
                CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(5000) // Wait 5 seconds
                    startPrintingQueue() // Try to resume printing
                }
            }
        }
    }

    private fun printLabel(label: LabelResponse) {
        val labelContent = buildLabelContent(label)
        printerHelper.printLabelContent(labelContent)
    }

    private fun buildLabelContent(label: LabelResponse): String {
        return buildString {
            appendLine("=== ${label.parsed_data.product_name} ===")
            appendLine("Batch: ${label.parsed_data.batch_no}")
            appendLine("Prepped: ${label.parsed_data.dates.getOrNull(0) ?: "N/A"}")
            appendLine("Use By: ${label.parsed_data.dates.getOrNull(1) ?: "N/A"}")
            appendLine("Employee: ${label.parsed_data.employee_name}")
            if (label.parsed_data.defrost_date != null) {
                appendLine("Defrosted: ${label.parsed_data.defrost_date}")
            }
            if (label.parsed_data.ready_to_prep_date != null) {
                appendLine("Ready to Prep: ${label.parsed_data.ready_to_prep_date}")
            }
            appendLine("Type: ${label.parsed_data.label_type}")
            if (label.parsed_data.rte_status != null) {
                appendLine("RTE Status: ${label.parsed_data.rte_status}")
            }
            appendLine("=====================")
        }
    }

    fun stopPrinting() {
        isPrinting = false
        printQueue.clear()
    }
} 