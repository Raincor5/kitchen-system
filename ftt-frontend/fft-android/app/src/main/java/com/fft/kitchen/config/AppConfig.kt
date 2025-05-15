package com.fft.kitchen.config

import com.fft.kitchen.BuildConfig

object AppConfig {
    private const val API_URL_DEBUG = "http://10.0.2.2:8000"  // Android emulator localhost
    private const val API_URL_RELEASE = "https://api.fft-kitchen.com"  // Replace with actual production URL
    
    fun getApiUrl(isDebug: Boolean): String {
        return if (isDebug) API_URL_DEBUG else API_URL_RELEASE
    }

    // Other configuration constants can be added here
} 