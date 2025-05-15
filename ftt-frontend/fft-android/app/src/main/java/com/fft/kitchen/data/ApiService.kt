package com.fft.kitchen.data

import com.fft.kitchen.LabelResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    @Multipart
    @POST("api/label-processor/process-image")
    fun processImage(@Part image: MultipartBody.Part): Call<List<LabelResponse>>

    @POST("api/label-processor/save-label")
    fun saveLabel(@Body label: LabelResponse): Call<Map<String, Any>>

    @GET("api/label-processor/get-labels")
    fun getLabels(): Call<List<LabelResponse>>

    @POST("api/label-processor/delete-label")
    fun deleteLabel(@Query("label_id") labelId: String): Call<Map<String, Any>>

    @POST("api/label-processor/print-saved-label/{labelId}")
    fun printSavedLabel(@Path("labelId") labelId: String): Call<Map<String, Any>>

    @GET
    suspend fun checkAwaken(@Url url: String): Response<Map<String, String>>
} 