package com.fft.kitchen

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fft.kitchen.printer.PrinterSettings
import com.fft.kitchen.printer.PrinterController
import android.graphics.Typeface
import android.view.Gravity
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextWatcher
import android.text.Editable

class LabelConfigActivity : AppCompatActivity() {
    private lateinit var printerSettings: PrinterSettings
    private lateinit var printerController: PrinterController
    
    // Preview views
    private lateinit var productNamePreview: TextView
    private lateinit var datePreview: TextView
    
    // SeekBars
    private lateinit var productNameSizeSeekBar: SeekBar
    private lateinit var dateSizeSeekBar: SeekBar
    
    // Text views for values
    private lateinit var productNameSizeText: TextView
    private lateinit var dateSizeText: TextView
    
    // Label size inputs
    private lateinit var labelWidthInput: EditText
    private lateinit var labelHeightInput: EditText
    
    // Alignment radio buttons
    private lateinit var alignmentGroup: RadioGroup
    private lateinit var alignLeft: RadioButton
    private lateinit var alignCenter: RadioButton
    private lateinit var alignRight: RadioButton
    
    // Buttons
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_config)
        
        // Initialize printer components
        printerSettings = PrinterSettings.getInstance(this)
        printerController = PrinterController(this)
        
        // Initialize UI components
        initializeViews()
        
        // Setup preview with sample data
        setupPreview()
        
        // Load current settings
        loadCurrentSettings()
        
        // Setup listeners
        setupListeners()
    }
    
    private fun initializeViews() {
        // Preview views
        productNamePreview = findViewById(R.id.labelName)
        datePreview = findViewById(R.id.labelDates)
        
        // SeekBars
        productNameSizeSeekBar = findViewById(R.id.productNameSizeSeekBar)
        dateSizeSeekBar = findViewById(R.id.dateSizeSeekBar)
        
        // Text views
        productNameSizeText = findViewById(R.id.productNameSizeText)
        dateSizeText = findViewById(R.id.dateSizeText)
        
        // Label size inputs
        labelWidthInput = findViewById(R.id.labelWidthInput)
        labelHeightInput = findViewById(R.id.labelHeightInput)
        
        // Alignment controls
        alignmentGroup = findViewById(R.id.alignmentGroup)
        alignLeft = findViewById(R.id.alignLeft)
        alignCenter = findViewById(R.id.alignCenter)
        alignRight = findViewById(R.id.alignRight)
        
        // Buttons
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetButton)
    }
    
    private fun setupPreview() {
        // Set sample data
        productNamePreview.text = "Sample Product"
        datePreview.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        
        // Set initial preview card width
        updatePreviewCardWidth(80) // 80% of max width
    }
    
    private fun loadCurrentSettings() {
        // Load font sizes
        val productNameSize = printerSettings.getFontSize()
        val dateSize = printerSettings.getFontSize() * 0.8f // Date is 80% of product name size
        
        productNameSizeSeekBar.progress = productNameSize.toInt()
        dateSizeSeekBar.progress = dateSize.toInt()
        
        // Load label dimensions
        val maxWidth = printerController.getMaxPrinterWidth()
        val currentWidth = printerSettings.getLabelWidth()
        val widthPercentage = (currentWidth.toFloat() / maxWidth * 100).toInt()
        labelWidthInput.setText(widthPercentage.toString())
        
        // Load label height (convert from mm to cm)
        val heightCm = printerSettings.getLabelHeight() / 10
        labelHeightInput.setText(heightCm.toString())
        
        // Load text alignment
        val alignment = printerSettings.getTextAlignment()
        when (alignment) {
            Gravity.START -> alignLeft.isChecked = true
            Gravity.CENTER -> alignCenter.isChecked = true
            Gravity.END -> alignRight.isChecked = true
        }
        
        // Update preview
        updatePreview()
    }
    
    private fun setupListeners() {
        // Product name size
        productNameSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                productNameSizeText.text = "${progress}sp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Date size
        dateSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dateSizeText.text = "${progress}sp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Label width
        labelWidthInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePreviewCardWidth(s?.toString()?.toInt() ?: 0)
                updatePreview()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Label height
        labelHeightInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePreview()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Text alignment
        alignmentGroup.setOnCheckedChangeListener { _, checkedId ->
            updatePreview()
        }
        
        // Save button
        saveButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        // Reset button
        resetButton.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun updatePreview() {
        // Update font sizes
        productNamePreview.textSize = productNameSizeSeekBar.progress.toFloat()
        datePreview.textSize = dateSizeSeekBar.progress.toFloat()
        
        // Update text alignment
        val alignment = when (alignmentGroup.checkedRadioButtonId) {
            R.id.alignLeft -> Gravity.START
            R.id.alignCenter -> Gravity.CENTER
            R.id.alignRight -> Gravity.END
            else -> Gravity.START
        }
        productNamePreview.gravity = alignment
        datePreview.gravity = alignment
    }
    
    private fun updatePreviewCardWidth(percentage: Int) {
        val maxWidth = printerController.getMaxPrinterWidth()
        val width = (maxWidth * percentage / 100)
        findViewById<androidx.cardview.widget.CardView>(R.id.previewCard).layoutParams.width = width
    }
    
    private fun saveSettings() {
        try {
            // Save font sizes
            printerSettings.setFontSize(productNameSizeSeekBar.progress.toFloat())
            
            // Save label dimensions with validation
            val maxWidth = printerController.getMaxPrinterWidth()
            val widthText = labelWidthInput.text.toString()
            val widthPercentage = if (widthText.isNotEmpty()) {
                widthText.toInt().coerceIn(1, 100)
            } else {
                80 // Default to 80% if empty
            }
            val width = (maxWidth * widthPercentage / 100)
            printerSettings.setLabelWidth(width)
            
            // Save label height with validation (convert from cm to mm)
            val heightText = labelHeightInput.text.toString()
            val heightCm = if (heightText.isNotEmpty()) {
                heightText.toInt().coerceIn(1, 100) // Limit height between 1-100cm
            } else {
                10 // Default to 10cm if empty
            }
            val heightMm = heightCm * 10
            printerSettings.setLabelHeight(heightMm)
            
            // Save text alignment
            val alignment = when (alignmentGroup.checkedRadioButtonId) {
                R.id.alignLeft -> Gravity.START
                R.id.alignCenter -> Gravity.CENTER
                R.id.alignRight -> Gravity.END
                else -> Gravity.START
            }
            printerSettings.setTextAlignment(alignment)
            
            // Save all settings
            printerSettings.saveSettings()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid input values. Using defaults.", Toast.LENGTH_SHORT).show()
            // Reset to defaults on error
            resetToDefaults()
        }
    }
    
    private fun resetToDefaults() {
        // Reset font sizes
        productNameSizeSeekBar.progress = 24
        dateSizeSeekBar.progress = 20
        
        // Reset label dimensions
        labelWidthInput.setText("80")
        labelHeightInput.setText("10")
        
        // Reset text alignment
        alignLeft.isChecked = true
        
        // Update preview
        updatePreview()
    }
} 