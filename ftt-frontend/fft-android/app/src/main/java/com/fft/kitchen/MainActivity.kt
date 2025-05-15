package com.fft.kitchen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ImageFormat
import android.widget.Toast
import androidx.camera.view.PreviewView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.fft.kitchen.utils.LabelAdapter
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.YuvImage
import android.graphics.Rect
import com.google.android.material.button.MaterialButton
import okhttp3.MultipartBody
import com.fft.kitchen.data.RetrofitClient
import android.view.ViewGroup
import com.fft.kitchen.printer.PrinterController
import com.fft.kitchen.printer.PrinterSettings
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val CAMERA_PERMISSION_CODE = 100
    private var isCameraActive = false
    private var snapshotJob: Job? = null
    private var printerController: PrinterController? = null

    private lateinit var loadingLayout: View
    private lateinit var loadingText: TextView
    private lateinit var viewFinder: PreviewView

    object LabelCache {
        val savedLabels = LinkedHashMap<String, LabelResponse>()
        val displayedLabels = LinkedHashMap<String, LabelResponse>()
    }

    private val labelMap = LinkedHashMap<String, LabelResponse>() // Storage for unique labels
    private lateinit var adapter: LabelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingText = findViewById(R.id.loadingText)
        viewFinder = findViewById(R.id.viewFinder)

        // Initialize adapter with empty list
        adapter = LabelAdapter(labelMap)
        
        // Note: RecyclerView will be initialized in LabelManagerActivity
        // We don't have a RecyclerView in MainActivity

        // Initialize and start the printer controller web server
        initializePrinterController()

        // Show loading screen with random duration
        simulateLoadingScreen()

        // Configure capture button
        findViewById<MaterialButton>(R.id.captureButton).setOnClickListener {
            if (isCameraActive) {
                // Use a coroutine for capture to handle suspension functions correctly
                captureAndProcessImage()
                
                // Show a brief flash effect
                val flashView = View(this).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(Color.WHITE)
                    alpha = 0.7f
                }
                
                val rootView = findViewById<ViewGroup>(android.R.id.content)
                rootView.addView(flashView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                
                flashView.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        rootView.removeView(flashView)
                    }
                    .start()
            } else {
                Toast.makeText(this, "Camera is not ready. Please wait...", Toast.LENGTH_SHORT).show()
                startCamera() // Try to restart the camera
            }
        }

        // Handle button click
        val showSavedLabelsButton = findViewById<MaterialButton>(R.id.showSavedLabelsButton)
        showSavedLabelsButton.setOnClickListener {
            val intent = Intent(this, LabelManagerActivity::class.java)
            startActivity(intent)
        }
        
        // Handle print text button click
        val printTextButton = findViewById<MaterialButton>(R.id.printTextButton)
        printTextButton.setOnClickListener {
            val intent = Intent(this, PrintTextActivity::class.java)
            startActivity(intent)
        }

        // Configure label button
        findViewById<Button>(R.id.configureLabelButton).setOnClickListener {
            startActivity(Intent(this, LabelLayoutDesignerActivity::class.java))
        }

        // Permission check
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }
        // Initialize Camera Executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Add QR scanner button
        findViewById<Button>(R.id.scanQRButton).setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivityForResult(intent, QR_SCANNER_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        stopSnapshots() // This is now just for cleanup
        isCameraActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSnapshots() // This is now just for cleanup
        cameraExecutor.shutdown()
        
        // Clean up printer controller
        printerController = null
        Log.d("PrinterController", "Printer controller cleaned up")
    }

    private fun stopSnapshots() {
        snapshotJob?.cancel()
        snapshotJob = null
    }

    private fun captureAndProcessImage() {
        // Disable the button temporarily
        findViewById<MaterialButton>(R.id.captureButton).isEnabled = false

        // Launch a coroutine to handle the image capture
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@MainActivity, "Processing your image...", Toast.LENGTH_SHORT).show()
                val scanResult = captureSnapshot()
                processImageResult(scanResult)
            } catch (e: Exception) {
                Log.e("ImageCapture", "Failed to process image", e)
                Toast.makeText(this@MainActivity, "Failed to process image: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable the button
                findViewById<MaterialButton>(R.id.captureButton).isEnabled = true
            }
        }
    }

    private suspend fun captureSnapshot(): List<LabelResponse> = withContext(Dispatchers.IO) {
        if (!isCameraActive) {
            Log.d("Camera", "Camera is not active, cannot capture snapshot")
            throw Exception("Camera is not active")
        }

        if (!::imageCapture.isInitialized) {
            Log.d("Camera", "ImageCapture is not initialized")
            throw Exception("Camera not initialized")
        }

        return@withContext suspendCancellableCoroutine { continuation ->
            val imageListener = object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    Log.d("Camera", "Image captured successfully")
                    
                    // Use a coroutine to process the image and call the suspending function
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Process the image
                            val jpegBytes = processImageProxy(imageProxy)
                            
                            // Upload to API and process response
                            val result = processImage(jpegBytes)
                            
                            // Close the image proxy and resume with result
                            imageProxy.close()
                            continuation.resume(result)
                        } catch (e: Exception) {
                            Log.e("Camera", "Error processing image", e)
                            imageProxy.close()
                            continuation.resumeWithException(e)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "Image capture failed", exception)
                    continuation.resumeWithException(exception)
                }
            }

            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this@MainActivity),
                imageListener
            )

            continuation.invokeOnCancellation {
                Log.d("Camera", "Image capture cancelled")
            }
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy): ByteArray {
        // This converts any camera image format to JPEG byte array
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        
        // Handle YUV format 
        if (imageProxy.format == ImageFormat.YUV_420_888) {
            val bitmap = imageToBitmap(imageProxy)
            val jpegStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, jpegStream)
            return jpegStream.toByteArray()
        }
        
        // Default case for JPEG format
        return data
    }
    
    private fun imageToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private suspend fun processImage(jpegData: ByteArray): List<LabelResponse> = withContext(Dispatchers.IO) {
        Log.d("API", "Sending image to API, size: ${jpegData.size} bytes")
        
        // Create multipart form data
        val requestBody = jpegData.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
        
        // Make network request
        val response = RetrofitClient.api.processImage(filePart).execute()
        
        if (response.isSuccessful && response.body() != null) {
            val labels = response.body()!!
            Log.d("API", "Received ${labels.size} labels")
            
            // Add to local cache
            for (label in labels) {
                val uniqueKey = label.uniqueKey ?: UUID.randomUUID().toString()
                LabelCache.displayedLabels[uniqueKey] = label
                labelMap[uniqueKey] = label
            }
            
            return@withContext labels
        } else {
            val errorMsg = "API Error: ${response.code()} - ${response.errorBody()?.string()}"
            Log.e("API", errorMsg)
            throw IOException(errorMsg)
        }
    }

    private fun processImageResult(labels: List<LabelResponse>) {
        // Update the adapter with new labels
        runOnUiThread {
            // Notify the adapter that data has changed
            adapter.notifyDataSetChanged()
            
            if (labels.isEmpty()) {
                Toast.makeText(this, "No labels found in image", Toast.LENGTH_SHORT).show()
            } else {
                val msg = "Found ${labels.size} label(s):\n" + 
                    labels.joinToString("\n") { it.parsed_data.product_name }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                
                // Start LabelManagerActivity to show the labels
                val intent = Intent(this, LabelManagerActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun simulateLoadingScreen() {
        loadingLayout.visibility = View.VISIBLE
        viewFinder.visibility = View.GONE

        // Get the server URL
        val settings = PrinterSettings.getInstance(this)
        val awakenUrl = settings.getAwakenUrl()

        if (awakenUrl.isEmpty()) {
            // No server URL configured, show QR scanner
            loadingText.text = "Please scan QR code"
            Handler(Looper.getMainLooper()).postDelayed({
                loadingLayout.visibility = View.GONE
                viewFinder.visibility = View.VISIBLE
                val intent = Intent(this, QRScannerActivity::class.java)
                startActivityForResult(intent, QR_SCANNER_REQUEST_CODE)
            }, 1000)
            return
        }

        // Initialize RetrofitClient with the server URL
        RetrofitClient.initialize(this)

        // Check server connection
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.checkAwaken(awakenUrl)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        loadingText.text = "Connected to server"
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadingLayout.visibility = View.GONE
                            viewFinder.visibility = View.VISIBLE
                            startCamera()
                        }, 1000)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingText.text = "Server connection failed"
                        Toast.makeText(this@MainActivity, "Failed to connect to server. Please try scanning the QR code again.", Toast.LENGTH_LONG).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@MainActivity, QRScannerActivity::class.java)
                            startActivityForResult(intent, QR_SCANNER_REQUEST_CODE)
                        }, 2000)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingText.text = "Connection error"
                    Toast.makeText(this@MainActivity, "Error connecting to server: ${e.message}", Toast.LENGTH_LONG).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@MainActivity, QRScannerActivity::class.java)
                        startActivityForResult(intent, QR_SCANNER_REQUEST_CODE)
                    }, 2000)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                // Image capture
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(windowManager.defaultDisplay.rotation)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                isCameraActive = true
                Log.d("Camera", "Camera started successfully")
                findViewById<MaterialButton>(R.id.captureButton).isEnabled = true

            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                isCameraActive = false
                findViewById<MaterialButton>(R.id.captureButton).isEnabled = false
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun initializePrinterController() {
        printerController = PrinterController(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to use this app", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_SCANNER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Server URL was updated, restart the loading process
                simulateLoadingScreen()
            } else {
                // User cancelled or failed to scan, show error and retry
                updateLoadingText("Server URL configuration required. Please scan QR code to continue.")
                Toast.makeText(this, "Server URL configuration is required", Toast.LENGTH_LONG).show()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000L)
                    startActivityForResult(Intent(this@MainActivity, QRScannerActivity::class.java), QR_SCANNER_REQUEST_CODE)
                }
            }
        }
    }

    private fun updateLoadingText(text: String) {
        runOnUiThread {
            loadingText.text = text
        }
    }

    private fun logLabelCache(tag: String) {
        Log.d("LabelCache", "$tag - Saved: ${LabelCache.savedLabels.size}, Displayed: ${LabelCache.displayedLabels.size}")
    }

    companion object {
        private const val QR_SCANNER_REQUEST_CODE = 1001
    }
}