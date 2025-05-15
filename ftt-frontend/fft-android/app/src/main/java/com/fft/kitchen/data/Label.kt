package com.fft.kitchen.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey val id: String,
    val dishName: String,
    val prepDate: Date,
    val expiryDate: Date,
    val ingredients: List<String>,
    val allergens: List<String>,
    val notes: String?,
    val trayId: String,
    val createdAt: Date = Date()
) 