package com.fft.kitchen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fft.kitchen.printer.PrinterSettings
import com.fft.kitchen.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class SplashActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check if we have a server URL
        val settings = PrinterSettings.getInstance(this)
        val serverUrl = settings.getServerUrl()

        if (serverUrl.isNullOrEmpty()) {
            // No server URL, go straight to QR scanner
            startQRScanner()
        } else {
            // Test existing connection
            lifecycleScope.launch {
                if (testConnection(serverUrl)) {
                    // Connection successful, go to main activity
                    startMainActivity()
                } else {
                    // Connection failed, clear URL and go to QR scanner
                    settings.clearServerUrl()
                    Toast.makeText(this@SplashActivity, "Could not connect to server. Please scan QR code again.", Toast.LENGTH_LONG).show()
                    startQRScanner()
                }
            }
        }
    }

    private fun startQRScanner() {
        startActivity(Intent(this, QRScannerActivity::class.java))
        finish()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private suspend fun testConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // First try the root endpoint
            var request = Request.Builder()
                .url(url)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Root endpoint check failed: ${response.code}")
                    // Try the health endpoint
                    request = Request.Builder()
                        .url("$url/health")
                        .build()
                    
                    client.newCall(request).execute().use { healthResponse ->
                        if (!healthResponse.isSuccessful) {
                            val errorBody = healthResponse.body?.string()
                            println("Health check failed: ${healthResponse.code} - $errorBody")
                            return@withContext false
                        }
                        return@withContext true
                    }
                }
                return@withContext true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("Connection error: ${e.message}")
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            println("Unexpected error: ${e.message}")
            return@withContext false
        }
    }
} 