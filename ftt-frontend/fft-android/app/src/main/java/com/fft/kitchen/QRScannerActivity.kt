package com.fft.kitchen

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import com.fft.kitchen.printer.PrinterSettings
import com.fft.kitchen.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class QRScannerActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        // Force portrait mode for this activity
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // Clear existing URL if this is a reconnection
        val settings = PrinterSettings.getInstance(this)
        settings.clearServerUrl()
        
        // Show instructions
        Toast.makeText(this, "Please scan the server URL QR code to connect to the server", Toast.LENGTH_LONG).show()

        // Initialize QR scanner with custom configuration
        val integrator = IntentIntegrator(this)
        integrator.apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan the server URL QR code")
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivity::class.java)
            initiateScan()
        }
    }

    private suspend fun testConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("Starting connection test to: $url")
            
            // First try the root endpoint
            var request = Request.Builder()
                .url(url)
                .build()
            
            println("Trying root endpoint...")
            client.newCall(request).execute().use { response ->
                println("Root endpoint response: ${response.code}")
                if (!response.isSuccessful) {
                    // Try the health endpoint
                    request = Request.Builder()
                        .url("$url/health")
                        .build()
                    
                    println("Trying health endpoint...")
                    client.newCall(request).execute().use { healthResponse ->
                        println("Health endpoint response: ${healthResponse.code}")
                        if (!healthResponse.isSuccessful) {
                            val errorBody = healthResponse.body?.string()
                            println("Health check failed: ${healthResponse.code} - $errorBody")
                            return@withContext false
                        }
                        println("Health check successful")
                        return@withContext true
                    }
                }
                println("Root endpoint check successful")
                return@withContext true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("Connection error: ${e.message}")
            println("Stack trace: ${e.stackTraceToString()}")
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            println("Unexpected error: ${e.message}")
            println("Stack trace: ${e.stackTraceToString()}")
            return@withContext false
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Validate the URL
                val url = result.contents.trim()  // Trim any whitespace
                println("Scanned URL: $url")
                
                if (url.startsWith("https://") && url.contains("ngrok.app")) {
                    // Show testing connection message
                    Toast.makeText(this, "Testing connection to server...", Toast.LENGTH_SHORT).show()
                    
                    // Test connection before saving
                    lifecycleScope.launch {
                        println("Testing connection to: $url")
                        val isConnected = testConnection(url)
                        if (isConnected) {
                            // Save the URL to settings
                            val settings = PrinterSettings.getInstance(this@QRScannerActivity)
                            settings.setServerUrl(url)
                            settings.saveSettings()
                            
                            // Update RetrofitClient with new URL
                            RetrofitClient.updateServerUrl(url)
                            
                            Toast.makeText(this@QRScannerActivity, "Server URL configured successfully", Toast.LENGTH_SHORT).show()
                            
                            // Return success
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val message = "Could not connect to server at $url. Please check if the server is running and try again."
                            println(message)
                            Toast.makeText(this@QRScannerActivity, message, Toast.LENGTH_LONG).show()
                            // Restart scanner
                            restartScanner()
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid server URL format. Please scan the correct QR code.", Toast.LENGTH_LONG).show()
                    // Restart scanner
                    restartScanner()
                }
            } else {
                // User cancelled, but we don't allow cancellation
                Toast.makeText(this, "Please scan the QR code to continue", Toast.LENGTH_LONG).show()
                // Restart scanner
                restartScanner()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun restartScanner() {
        val integrator = IntentIntegrator(this)
        integrator.apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan the server URL QR code")
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivity::class.java)
            initiateScan()
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        Toast.makeText(this, "Please scan the QR code to continue", Toast.LENGTH_LONG).show()
    }
}  