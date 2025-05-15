package com.fft.kitchen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fft.kitchen.printer.AidlPrinterHelper
import com.fft.kitchen.printer.PrinterSettings
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PrintTextActivity : AppCompatActivity() {
    private lateinit var printerHelper: AidlPrinterHelper
    private lateinit var settings: PrinterSettings

    private lateinit var textInput: TextInputEditText
    private lateinit var feedLinesInput: TextInputEditText
    private lateinit var printButton: MaterialButton
    private lateinit var calibrateButton: MaterialButton
    private lateinit var applyButton: MaterialButton
    private lateinit var configureLabelButton: MaterialButton
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isCalibrating = false
    private var isBinding = false
    private var connectionAttempts = 0
    private val MAX_CONNECTION_ATTEMPTS = 3

    companion object {
        // Static flag to prevent multiple unbinds
        private var isServiceGloballyBound = false
        
        // Keep track of active instances
        private var activeInstances = 0
        
        // Make sure the service stays bound across multiple instances
        @Synchronized
        fun incrementInstanceCount() {
            activeInstances++
        }
        
        @Synchronized
        fun decrementInstanceCount(): Int {
            if (activeInstances > 0) {
                activeInstances--
            }
            return activeInstances
        }
        
        @Synchronized
        fun getInstanceCount(): Int {
            return activeInstances
        }
        
        @Synchronized
        fun setServiceBound(bound: Boolean) {
            isServiceGloballyBound = bound
        }
        
        @Synchronized
        fun isServiceBound(): Boolean {
            return isServiceGloballyBound
        }
    }

    private val printerCallback = object : AidlPrinterHelper.PrinterCallback {
        override fun onConnected() {
            runOnUiThread {
                isBinding = false
                connectionAttempts = 0
                setServiceBound(true)
                printButton.isEnabled = true
                calibrateButton.isEnabled = true
                applyButton.isEnabled = true
                statusText.text = "Printer connected"
                Toast.makeText(this@PrintTextActivity, "Printer connected", Toast.LENGTH_SHORT).show()
                
                // Save connection state to preferences
                saveConnectionState(true)
            }
        }

        override fun onDisconnected() {
            runOnUiThread {
                isBinding = false
                setServiceBound(false)
                // Keep Print button enabled so user can reconnect
                printButton.isEnabled = true 
                calibrateButton.isEnabled = false
                statusText.text = "Printer disconnected"
                Toast.makeText(this@PrintTextActivity, "Printer disconnected", Toast.LENGTH_SHORT).show()
                
                // Save connection state to preferences
                saveConnectionState(false)
                
                // Try to reconnect if not intentionally unbound
                if (connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
                    connectionAttempts++
                    statusText.text = "Reconnecting: attempt $connectionAttempts..."
                    handler.postDelayed({
                        if (!printerHelper.isConnected() && !isBinding) {
                            forceConnectToPrinter()
                        }
                    }, 1000)
                }
            }
        }

        override fun onError(message: String) {
            runOnUiThread {
                isBinding = false
                // Keep Print button enabled so user can retry
                printButton.isEnabled = true
                calibrateButton.isEnabled = false
                statusText.text = "Error: $message"
                Toast.makeText(this@PrintTextActivity, "Printer error: $message", Toast.LENGTH_SHORT).show()
                
                // Try to reconnect on certain errors
                if (message.contains("bind") && connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
                    connectionAttempts++
                    statusText.text = "Reconnecting: attempt $connectionAttempts..."
                    handler.postDelayed({
                        if (!printerHelper.isConnected() && !isBinding) {
                            forceConnectToPrinter()
                        }
                    }, 1000)
                }
            }
        }
    }

    private val calibrationRunnable = object : Runnable {
        override fun run() {
            if (isCalibrating && printerHelper.isConnected()) {
                printerHelper.feedPaper()
                handler.postDelayed(this, 500)
            }
        }
    }
    
    // Apply feed lines value explicitly with button
    private fun applyFeedLinesValue() {
        try {
            val linesText = feedLinesInput.text.toString()
            val lines = linesText.toIntOrNull()?.coerceIn(1, 10) ?: 3
            settings.setLinesPerFeed(lines)
            statusText.text = "Lines per feed set to: $lines"
            Toast.makeText(this, "Lines per feed set to: $lines", Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            settings.setLinesPerFeed(3)
            feedLinesInput.setText("3")
            statusText.text = "Lines per feed reset to default: 3"
            Toast.makeText(this, "Invalid number. Lines per feed reset to 3", Toast.LENGTH_SHORT).show()
        }
    }

    private val feedLinesWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // No automatic application of feed lines value
            // Let the user press the Apply button instead
        }
    }
    
    private fun saveConnectionState(connected: Boolean) {
        val prefs = getSharedPreferences("PrinterPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("printer_was_connected", connected).apply()
    }
    
    private fun wasConnected(): Boolean {
        val prefs = getSharedPreferences("PrinterPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("printer_was_connected", false)
    }
    
    private fun forceConnectToPrinter() {
        if (!printerHelper.isConnected() && !isBinding) {
            isBinding = true
            statusText.text = "Connecting to printer..."
            try {
                printerHelper.bindService(printerCallback)
            } catch (e: Exception) {
                isBinding = false
                statusText.text = "Failed to connect: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_text)
        
        // Track this instance
        incrementInstanceCount()

        textInput = findViewById(R.id.textInput)
        feedLinesInput = findViewById(R.id.feedLinesInput)
        printButton = findViewById(R.id.printButton)
        calibrateButton = findViewById(R.id.calibrateButton)
        applyButton = findViewById(R.id.applyButton)
        configureLabelButton = findViewById(R.id.configureLabelButton)
        statusText = findViewById(R.id.statusText)
        
        // Initially disable buttons until connected
        printButton.isEnabled = false
        calibrateButton.isEnabled = false

        printerHelper = AidlPrinterHelper.getInstance(applicationContext)
        settings = PrinterSettings.getInstance(applicationContext)
        printerHelper.setPrinterCallback(printerCallback)

        // Initialize feed lines input with saved value
        val currentLines = settings.getLinesPerFeed()
        feedLinesInput.setText(currentLines.toString())
        statusText.text = "Lines per feed: $currentLines"

        feedLinesInput.addTextChangedListener(feedLinesWatcher)
        
        // Add Apply button click handler
        applyButton.setOnClickListener {
            applyFeedLinesValue()
        }

        printButton.setOnClickListener {
            if (!printerHelper.isConnected()) {
                forceConnectToPrinter()
                return@setOnClickListener
            }
            
            val text = textInput.text.toString()
            if (text.isNotEmpty()) {
                val feedLines = settings.getLinesPerFeed()
                statusText.text = "Printing with $feedLines lines feed"
                
                // Print the text directly
                printerHelper.printText(text)
            } else {
                Toast.makeText(this, "Please enter text to print", Toast.LENGTH_SHORT).show()
            }
        }

        calibrateButton.setOnTouchListener { _, event ->
            if (!printerHelper.isConnected()) {
                forceConnectToPrinter()
                return@setOnTouchListener false
            }
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isCalibrating = true
                    handler.post(calibrationRunnable)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isCalibrating = false
                    handler.removeCallbacks(calibrationRunnable)
                    true
                }
                else -> false
            }
        }

        // Configure Label Layout button
        configureLabelButton.setOnClickListener {
            val intent = Intent(this, LabelLayoutDesignerActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        
        statusText.text = "Checking printer connection..."
        
        if (printerHelper.isConnected()) {
            statusText.text = "Printer connected"
            printButton.isEnabled = true
            calibrateButton.isEnabled = true
            applyButton.isEnabled = true
        } else {
            // If previously connected or should be connected, try to connect
            if (wasConnected() || isServiceBound()) {
                forceConnectToPrinter()
            } else {
                statusText.text = "Printer disconnected. Tap Print to connect."
                // Always keep Print button enabled so user can connect
                printButton.isEnabled = true
                calibrateButton.isEnabled = false
                applyButton.isEnabled = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isCalibrating = false
        handler.removeCallbacks(calibrationRunnable)
        // DO NOT unbind service here
    }

    override fun onDestroy() {
        super.onDestroy()
        isCalibrating = false
        handler.removeCallbacksAndMessages(null)
        
        // Only unbind if this is the last instance
        if (decrementInstanceCount() == 0) {
            printerHelper.unbindService()
            setServiceBound(false)
        }
    }
} 