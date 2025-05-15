package com.fft.kitchen.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import woyou.aidlservice.jiuiv5.IWoyouService
import woyou.aidlservice.jiuiv5.ICallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PrinterService(private val context: Context) {
    private var woyouService: IWoyouService? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun printLabel(
        dishName: String,
        prepDate: Date,
        expiryDate: Date,
        ingredients: List<String>,
        allergens: List<String>,
        notes: String?,
        trayId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            woyouService?.let { printer ->
                // Print header
                printer.setAlignment(1, null) // Center alignment
                printer.setFontSize(2f, null) // Larger font for dish name
                printer.printText("$dishName\n", null)
                
                printer.setFontSize(1f, null) // Reset font size
                printer.printText("Tray ID: $trayId\n", null)
                
                printer.setAlignment(0, null) // Left alignment
                printer.printText("Prep Date: ${dateFormat.format(prepDate)}\n", null)
                printer.printText("Expiry Date: ${dateFormat.format(expiryDate)}\n", null)
                printer.printText("-------------------\n", null)
                
                // Print ingredients
                printer.printText("Ingredients:\n", null)
                ingredients.forEach { ingredient ->
                    printer.printText("• $ingredient\n", null)
                }
                
                // Print allergens if any
                if (allergens.isNotEmpty()) {
                    printer.printText("-------------------\n", null)
                    printer.printText("Allergens:\n", null)
                    allergens.forEach { allergen ->
                        printer.printText("• $allergen\n", null)
                    }
                }
                
                // Print notes if any
                notes?.let {
                    printer.printText("-------------------\n", null)
                    printer.printText("Notes:\n", null)
                    printer.printText("$it\n", null)
                }
                
                // Print footer
                printer.setAlignment(1, null) // Center alignment
                printer.printText("-------------------\n", null)
                printer.printText("Generated: ${dateFormat.format(Date())}\n", null)
                printer.lineWrap(3, null) // Add some space at the end
                
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setWoyouService(service: IWoyouService) {
        woyouService = service
    }

    fun isPrinterConnected(): Boolean {
        return woyouService != null
    }

    fun getPrinterStatus(): String {
        return try {
            if (woyouService != null) {
                "Connected"
            } else {
                "Not Connected"
            }
        } catch (e: Exception) {
            "Error"
        }
    }
} 