package com.fft.kitchen.data

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.fft.kitchen.printer.PrinterSettings

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var instance: ApiService? = null
    private var currentUrl: String = ""

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun initialize(context: Context) {
        val settings = PrinterSettings.getInstance(context)
        val serverUrl = settings.getServerUrl()
        updateServerUrl(serverUrl)
    }

    fun updateServerUrl(newUrl: String) {
        if (newUrl.isEmpty()) {
            retrofit = null
            instance = null
            currentUrl = ""
            return
        }

        // Only recreate if URL has changed
        if (newUrl != currentUrl) {
            currentUrl = newUrl
            retrofit = createRetrofit(newUrl)
            instance = retrofit?.create(ApiService::class.java)
        }
    }

    val api: ApiService
        get() = instance ?: throw IllegalStateException("RetrofitClient must be initialized with a valid server URL first")
}
