package com.fft.kitchen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fft.kitchen.printer.AidlPrinterHelper
import com.fft.kitchen.printer.PrinterSettings
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import yuku.ambilwarna.AmbilWarnaDialog

class PrinterSettingsActivity : AppCompatActivity() {
    private lateinit var printerHelper: AidlPrinterHelper
    private lateinit var settings: PrinterSettings
    
    private lateinit var profileDropdown: AutoCompleteTextView
    private lateinit var feedLinesInput: TextInputEditText
    private lateinit var fontTypeDropdown: AutoCompleteTextView
    private lateinit var fontSizeInput: TextInputEditText
    private lateinit var labelWidthInput: TextInputEditText
    private lateinit var labelHeightInput: TextInputEditText
    private lateinit var statusText: TextView
    private lateinit var testFeedButton: MaterialButton
    private lateinit var applyButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var saveProfileButton: MaterialButton
    private lateinit var printDensityInput: TextInputEditText
    private lateinit var printSpeedInput: TextInputEditText
    private lateinit var printModeDropdown: AutoCompleteTextView
    private lateinit var customTextSwitch: SwitchMaterial
    private lateinit var customTextPositionDropdown: AutoCompleteTextView
    private lateinit var customTextSizeInput: TextInputEditText
    private lateinit var customTextInput: TextInputEditText
    private lateinit var customTextLayout: TextInputLayout
    private lateinit var customTextPositionLayout: TextInputLayout
    private lateinit var customTextSizeLayout: TextInputLayout
    private lateinit var backgroundColorButton: MaterialButton
    private lateinit var textColorButton: MaterialButton

    // Preview elements
    private lateinit var previewProductName: TextView
    private lateinit var previewBatchNo: TextView
    private lateinit var previewEmployee: TextView
    private lateinit var previewPrepLabel: TextView
    private lateinit var previewPrepDate: TextView
    private lateinit var previewUseByLabel: TextView
    private lateinit var previewUseByDate: TextView
    private lateinit var previewUseByDay: TextView
    private lateinit var previewCustomText: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var isFeeding = false
    
    // Available profiles - to be persisted and loaded
    private val profilesList = mutableListOf("default", "compact", "receipt", "shipping")
    
    private lateinit var printerCallback: AidlPrinterHelper.PrinterCallback
    
    private val feedRunnable = object : Runnable {
        override fun run() {
            if (isFeeding && printerHelper.isConnected()) {
                val feedLines = parseIntSafely(feedLinesInput.text.toString(), 1)
                printerHelper.feedPaper(feedLines.toFloat())
                handler.postDelayed(this, 500)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_settings)
        
        // Initialize the printer helper and settings
        printerHelper = AidlPrinterHelper.getInstance(applicationContext)
        settings = PrinterSettings.getInstance(applicationContext)
        
        // Initialize UI elements
        initializeViews()
        setupListeners()
        
        // Connect to printer service
        connectToPrinter()
        
        // Load current settings
        loadCurrentSettings()

        // Configure label layout button
        findViewById<Button>(R.id.configureLabelLayoutButton).setOnClickListener {
            startActivity(Intent(this, LabelLayoutDesignerActivity::class.java))
        }
    }
    
    private fun initializeViews() {
        profileDropdown = findViewById(R.id.profileDropdown)
        feedLinesInput = findViewById(R.id.feedLinesInput)
        fontTypeDropdown = findViewById(R.id.fontTypeDropdown)
        fontSizeInput = findViewById(R.id.fontSizeInput)
        labelWidthInput = findViewById(R.id.labelWidthInput)
        labelHeightInput = findViewById(R.id.labelHeightInput)
        statusText = findViewById(R.id.statusText)
        testFeedButton = findViewById(R.id.testFeedButton)
        applyButton = findViewById(R.id.applyButton)
        resetButton = findViewById(R.id.resetButton)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        printDensityInput = findViewById(R.id.printDensityInput)
        printSpeedInput = findViewById(R.id.printSpeedInput)
        printModeDropdown = findViewById(R.id.printModeDropdown)
        customTextSwitch = findViewById(R.id.customTextSwitch)
        customTextPositionDropdown = findViewById(R.id.customTextPositionDropdown)
        customTextSizeInput = findViewById(R.id.customTextSizeInput)
        customTextInput = findViewById(R.id.customTextInput)
        customTextLayout = findViewById(R.id.customTextLayout)
        customTextPositionLayout = findViewById(R.id.customTextPositionLayout)
        customTextSizeLayout = findViewById(R.id.customTextSizeLayout)
        backgroundColorButton = findViewById(R.id.backgroundColorButton)
        textColorButton = findViewById(R.id.textColorButton)

        // Initialize preview elements
        previewProductName = findViewById(R.id.previewProductName)
        previewBatchNo = findViewById(R.id.previewBatchNo)
        previewEmployee = findViewById(R.id.previewEmployee)
        previewPrepLabel = findViewById(R.id.previewPrepLabel)
        previewPrepDate = findViewById(R.id.previewPrepDate)
        previewUseByLabel = findViewById(R.id.previewUseByLabel)
        previewUseByDate = findViewById(R.id.previewUseByDate)
        previewUseByDay = findViewById(R.id.previewUseByDay)
        previewCustomText = findViewById(R.id.previewCustomText)
        
        // Set up adapters
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
            PrinterSettings.AVAILABLE_FONTS)
        fontTypeDropdown.setAdapter(fontAdapter)
        
        val profileAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
            profilesList)
        profileDropdown.setAdapter(profileAdapter)
        
        val printModeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
            PrinterSettings.AVAILABLE_PRINT_MODES)
        printModeDropdown.setAdapter(printModeAdapter)

        val textPositionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            PrinterSettings.AVAILABLE_TEXT_POSITIONS)
        customTextPositionDropdown.setAdapter(textPositionAdapter)
    }
    
    private fun setupListeners() {
        // Apply button
        applyButton.setOnClickListener {
            saveSettings()
            updatePreview()
        }
        
        // Reset button
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Settings")
                .setMessage("Are you sure you want to reset all settings to default values?")
                .setPositiveButton("Yes") { _, _ ->
                    settings.resetToDefaults()
                    loadCurrentSettings()
                    updatePreview()
                    Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
        
        // Save Profile button
        saveProfileButton.setOnClickListener {
            val currentProfile = profileDropdown.text.toString()
            if (currentProfile.isBlank()) {
                Toast.makeText(this, "Please select or enter a profile name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Add to profiles list if it's a new one
            if (!profilesList.contains(currentProfile)) {
                profilesList.add(currentProfile)
                // Update adapter
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, profilesList)
                profileDropdown.setAdapter(adapter)
                
                // Save the list of profiles to preferences
                saveProfilesList()
            }
            
            // Set as current profile and save settings
            settings.setCurrentProfile(currentProfile)
            saveSettings()
            
            Toast.makeText(this, "Profile '$currentProfile' saved", Toast.LENGTH_SHORT).show()
            statusText.text = "Current profile: $currentProfile"
        }
        
        // Profile dropdown selection
        profileDropdown.setOnItemClickListener { _, _, _, _ ->
            val selectedProfile = profileDropdown.text.toString()
            settings.setCurrentProfile(selectedProfile)
            loadCurrentSettings()
            updatePreview()
            statusText.text = "Loaded profile: $selectedProfile"
        }
        
        // Font type dropdown
        fontTypeDropdown.setOnItemClickListener { _, _, _, _ ->
            updatePreview()
        }
        
        // Font size change
        fontSizeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updatePreview()
            }
        })
        
        // Test feed button
        testFeedButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (printerHelper.isConnected()) {
                        isFeeding = true
                        handler.post(feedRunnable)
                    } else {
                        Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isFeeding = false
                    handler.removeCallbacks(feedRunnable)
                    true
                }
                else -> false
            }
        }
        
        // Print density change
        printDensityInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val density = parseIntSafely(s.toString(), 50)
                if (density in 0..100) {
                    settings.setPrintDensity(density)
                }
            }
        })

        // Print speed change
        printSpeedInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val speed = parseIntSafely(s.toString(), 2)
                if (speed in 1..5) {
                    settings.setPrintSpeed(speed)
                }
            }
        })

        // Print mode selection
        printModeDropdown.setOnItemClickListener { _, _, _, _ ->
            val selectedMode = printModeDropdown.text.toString()
            settings.setPrintMode(selectedMode)
        }

        // Custom text switch
        customTextSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setCustomTextEnabled(isChecked)
            customTextLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            customTextPositionLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            customTextSizeLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            previewCustomText.visibility = if (isChecked) View.VISIBLE else View.GONE
            updatePreview()
        }

        // Custom text position selection
        customTextPositionDropdown.setOnItemClickListener { _, _, _, _ ->
            val selectedPosition = customTextPositionDropdown.text.toString()
            settings.setCustomTextPosition(selectedPosition)
            updatePreview()
        }

        // Custom text size change
        customTextSizeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val size = parseFloatSafely(s.toString(), 12f)
                settings.setCustomTextSize(size)
                updatePreview()
            }
        })

        // Custom text content change
        customTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val content = s.toString()
                settings.setCustomTextContent(content)
                updatePreview()
            }
        })

        // Background color button
        backgroundColorButton.setOnClickListener {
            val currentColor = settings.getLabelBackgroundColor()
            AmbilWarnaDialog(this, currentColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {}
                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        settings.setLabelBackgroundColor(color)
                        updatePreview()
                    }
                }).show()
        }

        // Text color button
        textColorButton.setOnClickListener {
            val currentColor = settings.getLabelTextColor()
            AmbilWarnaDialog(this, currentColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {}
                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        settings.setLabelTextColor(color)
                        updatePreview()
                    }
                }).show()
        }
    }
    
    private fun connectToPrinter() {
        statusText.text = "Connecting to printer..."
        
        printerCallback = object : AidlPrinterHelper.PrinterCallback {
            override fun onConnected() {
                runOnUiThread {
                    statusText.text = "Printer connected"
                    testFeedButton.isEnabled = true
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    statusText.text = "Printer disconnected"
                    testFeedButton.isEnabled = false
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    statusText.text = "Error: $message"
                    testFeedButton.isEnabled = false
                }
            }
        }
        
        if (!printerHelper.isConnected()) {
            printerHelper.bindService(printerCallback)
        } else {
            statusText.text = "Printer already connected"
            testFeedButton.isEnabled = true
        }
    }
    
    private fun loadCurrentSettings() {
        // Load the current profile
        val currentProfile = settings.getCurrentProfile()
        profileDropdown.setText(currentProfile, false)
        
        // Load feed lines
        feedLinesInput.setText(settings.getLinesPerFeed().toString())
        
        // Load font settings
        fontTypeDropdown.setText(settings.getFontName(), false)
        fontSizeInput.setText(settings.getFontSize().toString())
        
        // Load label dimensions
        labelWidthInput.setText(settings.getLabelWidth().toString())
        labelHeightInput.setText(settings.getLabelHeight().toString())
        
        // Load print quality settings
        printDensityInput.setText(settings.getPrintDensity().toString())
        printSpeedInput.setText(settings.getPrintSpeed().toString())
        printModeDropdown.setText(settings.getPrintMode(), false)

        // Load custom text settings
        customTextSwitch.isChecked = settings.isCustomTextEnabled()
        customTextPositionDropdown.setText(settings.getCustomTextPosition(), false)
        customTextSizeInput.setText(settings.getCustomTextSize().toString())
        customTextInput.setText(settings.getCustomTextContent())

        // Update visibility based on custom text enabled state
        customTextLayout.visibility = if (settings.isCustomTextEnabled()) View.VISIBLE else View.GONE
        customTextPositionLayout.visibility = if (settings.isCustomTextEnabled()) View.VISIBLE else View.GONE
        customTextSizeLayout.visibility = if (settings.isCustomTextEnabled()) View.VISIBLE else View.GONE
        previewCustomText.visibility = if (settings.isCustomTextEnabled()) View.VISIBLE else View.GONE
        
        // Update preview
        updatePreview()
    }
    
    private fun saveSettings() {
        try {
            // Save feed lines
            val feedLines = parseIntSafely(feedLinesInput.text.toString(), 3)
            settings.setLinesPerFeed(feedLines)
            
            // Save font settings
            val fontSize = parseFloatSafely(fontSizeInput.text.toString(), 12f)
            settings.setFontSize(fontSize)
            settings.setFontName(fontTypeDropdown.text.toString())
            
            // Save label dimensions
            val labelWidth = parseIntSafely(labelWidthInput.text.toString(), 40)
            val labelHeight = parseIntSafely(labelHeightInput.text.toString(), 30)
            settings.setLabelWidth(labelWidth)
            settings.setLabelHeight(labelHeight)
            
            // Save print quality settings
            val density = parseIntSafely(printDensityInput.text.toString(), 50)
            settings.setPrintDensity(density)
            val speed = parseIntSafely(printSpeedInput.text.toString(), 2)
            settings.setPrintSpeed(speed)
            val mode = printModeDropdown.text.toString()
            settings.setPrintMode(mode)

            // Save custom text settings
            settings.setCustomTextEnabled(customTextSwitch.isChecked)
            settings.setCustomTextPosition(customTextPositionDropdown.text.toString())
            settings.setCustomTextSize(parseFloatSafely(customTextSizeInput.text.toString(), 12f))
            settings.setCustomTextContent(customTextInput.text.toString())
            
            // Save all settings
            settings.saveSettings()
            
            Toast.makeText(this, "Settings saved for profile: ${settings.getCurrentProfile()}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updatePreview() {
        val fontName = fontTypeDropdown.text.toString()
        val fontSize = parseFloatSafely(fontSizeInput.text.toString(), 12f)
        
        // Update typeface for all preview elements
        val typeface = when (fontName.lowercase()) {
            "monospace" -> android.graphics.Typeface.MONOSPACE
            "sans serif" -> android.graphics.Typeface.SANS_SERIF
            "serif" -> android.graphics.Typeface.SERIF
            else -> android.graphics.Typeface.DEFAULT
        }
        
        // Apply font settings to all preview elements
        val previewElements = listOf(
            previewProductName,
            previewBatchNo,
            previewEmployee,
            previewPrepLabel,
            previewPrepDate,
            previewUseByLabel,
            previewUseByDate,
            previewUseByDay,
            previewCustomText
        )
        
        // Update background and text colors
        val backgroundColor = settings.getLabelBackgroundColor()
        val textColor = settings.getLabelTextColor()
        previewElements.forEach { textView ->
            textView.setBackgroundColor(backgroundColor)
            textView.setTextColor(textColor)
            textView.typeface = typeface
            // Adjust font size based on the element's role while maintaining proportions
            when (textView) {
                previewProductName -> textView.textSize = fontSize * 1.5f
                previewBatchNo, previewEmployee -> textView.textSize = fontSize * 0.8f
                previewUseByDate, previewUseByDay -> textView.textSize = fontSize * 1.2f
                previewCustomText -> textView.textSize = settings.getCustomTextSize()
                else -> textView.textSize = fontSize
            }
        }

        // Update sample data
        val currentTimeMillis = System.currentTimeMillis()
        val prepDate = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
            .format(currentTimeMillis)
        val useByDate = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
            .format(currentTimeMillis + (3 * 24 * 60 * 60 * 1000)) // 3 days later
        val useByDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            .format(currentTimeMillis + (3 * 24 * 60 * 60 * 1000)).uppercase()

        previewPrepDate.text = prepDate
        previewUseByDate.text = "$useByDate E.O.D"
        previewUseByDay.text = useByDay

        // Update custom text
        if (settings.isCustomTextEnabled()) {
            previewCustomText.text = settings.getCustomTextContent()
            // Position custom text based on selected position
            when (settings.getCustomTextPosition()) {
                "Top" -> previewCustomText.translationY = 0f
                "Bottom" -> previewCustomText.translationY = previewUseByDay.bottom.toFloat()
                "Left" -> previewCustomText.translationX = 0f
                "Right" -> previewCustomText.translationX = previewProductName.right.toFloat()
            }
        }
    }
    
    private fun saveProfilesList() {
        val prefs = getSharedPreferences("PrinterPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet("saved_profiles", profilesList.toSet())
        editor.apply()
    }
    
    private fun loadProfilesList() {
        val prefs = getSharedPreferences("PrinterPrefs", Context.MODE_PRIVATE)
        val savedProfiles = prefs.getStringSet("saved_profiles", null)
        if (savedProfiles != null) {
            profilesList.clear()
            profilesList.addAll(savedProfiles)
            
            // Update adapter
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, profilesList)
            profileDropdown.setAdapter(adapter)
        }
    }
    
    private fun parseIntSafely(value: String, default: Int): Int {
        return try {
            value.toInt().coerceIn(1, 10)
        } catch (e: NumberFormatException) {
            default
        }
    }
    
    private fun parseFloatSafely(value: String, default: Float): Float {
        return try {
            value.toFloat()
        } catch (e: NumberFormatException) {
            default
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Load profiles
        loadProfilesList()
        
        // Check printer connection
        if (!printerHelper.isConnected()) {
            connectToPrinter()
        }
    }
    
    override fun onPause() {
        super.onPause()
        isFeeding = false
        handler.removeCallbacks(feedRunnable)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        printerHelper.unbindService()
    }
} 