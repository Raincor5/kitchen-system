package com.fft.kitchen

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fft.kitchen.data.LabelElement
import com.fft.kitchen.data.LabelLayout
import com.fft.kitchen.data.FieldType
import com.fft.kitchen.printer.PrinterSettings
import com.fft.kitchen.views.LabelPreviewView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LabelLayoutDesignerActivity : AppCompatActivity() {
    private lateinit var labelPreview: LabelPreviewView
    private lateinit var currentLayout: LabelLayout
    private var selectedElement: LabelElement? = null
    private lateinit var settings: PrinterSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_layout_designer)

        settings = PrinterSettings.getInstance(this)
        currentLayout = LabelLayout.createDefault()

        // Initialize views
        labelPreview = findViewById(R.id.labelPreview)
        labelPreview.setLayout(currentLayout)

        // Add element button
        findViewById<FloatingActionButton>(R.id.addElementButton).setOnClickListener {
            showAddElementDialog()
        }

        // Save layout button
        findViewById<Button>(R.id.saveLayoutButton).setOnClickListener {
            saveLayout()
        }

        // Font size controls
        findViewById<SeekBar>(R.id.fontSizeSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedElement?.let {
                    it.fontSize = progress.toFloat()
                    labelPreview.invalidate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showAddElementDialog() {
        val items = arrayOf("Product Name", "Batch Number", "Employee Name", "Prep Date", "Use By Date", "Custom Text")
        AlertDialog.Builder(this)
            .setTitle("Add Element")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> addElement(LabelElement("product_name_${System.currentTimeMillis()}", "Product Name", 10f, 10f, 24f, true, FieldType.PRODUCT_NAME))
                    1 -> addElement(LabelElement("batch_${System.currentTimeMillis()}", "Batch: ", 10f, 40f, 18f, false, FieldType.BATCH_NO))
                    2 -> addElement(LabelElement("employee_${System.currentTimeMillis()}", "Prep by: ", 10f, 70f, 18f, false, FieldType.EMPLOYEE_NAME))
                    3 -> addElement(LabelElement("prep_date_${System.currentTimeMillis()}", "Prep: ", 10f, 100f, 18f, false, FieldType.PREP_DATE))
                    4 -> addElement(LabelElement("use_by_${System.currentTimeMillis()}", "Use by: ", 10f, 130f, 18f, false, FieldType.USE_BY_DATE))
                    5 -> showCustomTextDialog()
                }
            }
            .show()
    }

    private fun addElement(element: LabelElement) {
        currentLayout.elements.add(element)
        labelPreview.invalidate()
    }

    private fun showCustomTextDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Enter Custom Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    val element = LabelElement(
                        "custom_${System.currentTimeMillis()}",
                        text,
                        10f,
                        10f,
                        18f,
                        false,
                        FieldType.CUSTOM_TEXT
                    )
                    currentLayout.elements.add(element)
                    labelPreview.invalidate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveLayout() {
        // Save layout to settings
        settings.setLabelLayout(currentLayout)
        Toast.makeText(this, "Layout saved", Toast.LENGTH_SHORT).show()
        finish()
    }
} 