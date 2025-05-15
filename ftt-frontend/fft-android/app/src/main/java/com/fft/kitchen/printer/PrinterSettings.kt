package com.fft.kitchen.printer

import android.content.Context
import android.content.SharedPreferences
import com.fft.kitchen.data.LabelLayout
import com.google.gson.Gson

class PrinterSettings private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "PrinterSettings"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_NAME = "font_name"
        private const val KEY_LABEL_WIDTH = "label_width"
        private const val KEY_LABEL_HEIGHT = "label_height"
        private const val KEY_TEXT_ALIGNMENT = "text_alignment"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LINES_PER_FEED = "lines_per_feed"
        private const val KEY_CURRENT_PROFILE = "current_profile"
        private const val KEY_LABEL_LAYOUT = "label_layout"
        private const val KEY_PRINT_DENSITY = "print_density"
        private const val KEY_PRINT_SPEED = "print_speed"
        private const val KEY_PRINT_MODE = "print_mode"
        private const val KEY_CUSTOM_TEXT_ENABLED = "custom_text_enabled"
        private const val KEY_CUSTOM_TEXT_POSITION = "custom_text_position"
        private const val KEY_CUSTOM_TEXT_SIZE = "custom_text_size"
        private const val KEY_CUSTOM_TEXT_CONTENT = "custom_text_content"
        private const val KEY_LABEL_BACKGROUND_COLOR = "label_background_color"
        private const val KEY_LABEL_TEXT_COLOR = "label_text_color"

        val AVAILABLE_FONTS = listOf("Default", "Monospace", "Sans Serif", "Serif")
        val AVAILABLE_PRINT_MODES = listOf("Standard", "Bold", "Compressed")
        val AVAILABLE_TEXT_POSITIONS = listOf("Top", "Bottom", "Left", "Right")
        
        @Volatile
        private var instance: PrinterSettings? = null
        
        @JvmStatic
        fun getInstance(context: Context): PrinterSettings {
            return instance ?: synchronized(this) {
                instance ?: PrinterSettings(context).also { instance = it }
            }
        }
    }

    fun setServerUrl(url: String) {
        // Remove trailing slash if present
        val cleanUrl = url.trimEnd('/')
        prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, "") ?: ""
    }

    fun getApiUrl(): String {
        val baseUrl = getServerUrl()
        return if (baseUrl.isNotEmpty()) {
            "$baseUrl/api/android"
        } else {
            ""
        }
    }

    fun getAwakenUrl(): String {
        val apiUrl = getApiUrl()
        return if (apiUrl.isNotEmpty()) {
            "$apiUrl/awaken"
        } else {
            ""
        }
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()
    }

    fun getFontSize(): Float {
        return prefs.getFloat(KEY_FONT_SIZE, 12f)
    }

    fun setFontName(name: String) {
        prefs.edit().putString(KEY_FONT_NAME, name).apply()
    }

    fun getFontName(): String {
        return prefs.getString(KEY_FONT_NAME, "Default") ?: "Default"
    }

    fun setLabelWidth(width: Int) {
        prefs.edit().putInt(KEY_LABEL_WIDTH, width).apply()
    }

    fun getLabelWidth(): Int {
        return prefs.getInt(KEY_LABEL_WIDTH, 40)
    }

    fun setLabelHeight(height: Int) {
        prefs.edit().putInt(KEY_LABEL_HEIGHT, height).apply()
    }

    fun getLabelHeight(): Int {
        return prefs.getInt(KEY_LABEL_HEIGHT, 30)
    }

    fun setTextAlignment(alignment: Int) {
        prefs.edit().putInt(KEY_TEXT_ALIGNMENT, alignment).apply()
    }

    fun getTextAlignment(): Int {
        return prefs.getInt(KEY_TEXT_ALIGNMENT, 0)
    }

    fun setLinesPerFeed(lines: Int) {
        prefs.edit().putInt(KEY_LINES_PER_FEED, lines).apply()
    }

    fun getLinesPerFeed(): Int {
        return prefs.getInt(KEY_LINES_PER_FEED, 3)
    }

    fun setCurrentProfile(profile: String) {
        prefs.edit().putString(KEY_CURRENT_PROFILE, profile).apply()
    }

    fun getCurrentProfile(): String {
        return prefs.getString(KEY_CURRENT_PROFILE, "Default") ?: "Default"
    }

    fun setLabelLayout(layout: LabelLayout) {
        val gson = Gson()
        val json = gson.toJson(layout)
        prefs.edit().putString(KEY_LABEL_LAYOUT, json).apply()
    }

    fun getLabelLayout(): LabelLayout {
        val gson = Gson()
        val json = prefs.getString(KEY_LABEL_LAYOUT, null)
        return if (json != null) {
            try {
                gson.fromJson(json, LabelLayout::class.java)
            } catch (e: Exception) {
                LabelLayout.createDefault()
            }
        } else {
            LabelLayout.createDefault()
        }
    }

    fun setPrintDensity(density: Int) {
        prefs.edit().putInt(KEY_PRINT_DENSITY, density).apply()
    }

    fun getPrintDensity(): Int {
        return prefs.getInt(KEY_PRINT_DENSITY, 50)
    }

    fun setPrintSpeed(speed: Int) {
        prefs.edit().putInt(KEY_PRINT_SPEED, speed).apply()
    }

    fun getPrintSpeed(): Int {
        return prefs.getInt(KEY_PRINT_SPEED, 2)
    }

    fun setPrintMode(mode: String) {
        prefs.edit().putString(KEY_PRINT_MODE, mode).apply()
    }

    fun getPrintMode(): String {
        return prefs.getString(KEY_PRINT_MODE, "Standard") ?: "Standard"
    }

    fun setCustomTextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CUSTOM_TEXT_ENABLED, enabled).apply()
    }

    fun isCustomTextEnabled(): Boolean {
        return prefs.getBoolean(KEY_CUSTOM_TEXT_ENABLED, false)
    }

    fun setCustomTextPosition(position: String) {
        prefs.edit().putString(KEY_CUSTOM_TEXT_POSITION, position).apply()
    }

    fun getCustomTextPosition(): String {
        return prefs.getString(KEY_CUSTOM_TEXT_POSITION, "Bottom") ?: "Bottom"
    }

    fun setCustomTextSize(size: Float) {
        prefs.edit().putFloat(KEY_CUSTOM_TEXT_SIZE, size).apply()
    }

    fun getCustomTextSize(): Float {
        return prefs.getFloat(KEY_CUSTOM_TEXT_SIZE, 12f)
    }

    fun setCustomTextContent(content: String) {
        prefs.edit().putString(KEY_CUSTOM_TEXT_CONTENT, content).apply()
    }

    fun getCustomTextContent(): String {
        return prefs.getString(KEY_CUSTOM_TEXT_CONTENT, "") ?: ""
    }

    fun setLabelBackgroundColor(color: Int) {
        prefs.edit().putInt(KEY_LABEL_BACKGROUND_COLOR, color).apply()
    }

    fun getLabelBackgroundColor(): Int {
        return prefs.getInt(KEY_LABEL_BACKGROUND_COLOR, android.graphics.Color.rgb(79, 195, 247)) // #4FC3F7
    }

    fun setLabelTextColor(color: Int) {
        prefs.edit().putInt(KEY_LABEL_TEXT_COLOR, color).apply()
    }

    fun getLabelTextColor(): Int {
        return prefs.getInt(KEY_LABEL_TEXT_COLOR, android.graphics.Color.BLACK)
    }

    fun resetToDefaults() {
        prefs.edit().apply {
            putFloat(KEY_FONT_SIZE, 12f)
            putString(KEY_FONT_NAME, "Default")
            putInt(KEY_LABEL_WIDTH, 40)
            putInt(KEY_LABEL_HEIGHT, 30)
            putInt(KEY_TEXT_ALIGNMENT, 0)
            putInt(KEY_LINES_PER_FEED, 3)
            putString(KEY_CURRENT_PROFILE, "Default")
            putInt(KEY_PRINT_DENSITY, 50)
            putInt(KEY_PRINT_SPEED, 2)
            putString(KEY_PRINT_MODE, "Standard")
            putBoolean(KEY_CUSTOM_TEXT_ENABLED, false)
            putString(KEY_CUSTOM_TEXT_POSITION, "Bottom")
            putFloat(KEY_CUSTOM_TEXT_SIZE, 12f)
            putString(KEY_CUSTOM_TEXT_CONTENT, "")
            putInt(KEY_LABEL_BACKGROUND_COLOR, android.graphics.Color.rgb(79, 195, 247))
            putInt(KEY_LABEL_TEXT_COLOR, android.graphics.Color.BLACK)
        }.apply()
    }

    fun saveSettings() {
        // Save any pending changes
        prefs.edit().apply()
    }

    fun clearServerUrl() {
        prefs.edit().remove(KEY_SERVER_URL).apply()
    }
} 