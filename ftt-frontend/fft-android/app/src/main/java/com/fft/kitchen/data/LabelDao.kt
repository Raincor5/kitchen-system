package com.fft.kitchen.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY createdAt DESC")
    fun getAllLabels(): Flow<List<Label>>
    
    @Query("SELECT * FROM labels WHERE id = :id")
    suspend fun getLabelById(id: String): Label?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: Label)
    
    @Delete
    suspend fun deleteLabel(label: Label)
    
    @Query("DELETE FROM labels")
    suspend fun deleteAllLabels()
} 