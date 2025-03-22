package com.dicoding.asclepius.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_history")
data class PredictionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Int = 0,
    @ColumnInfo("session_name")
    val sessionName: String,
    @ColumnInfo("image_uri")
    val imageUri: String,
    @ColumnInfo("label")
    val label: String,
    @ColumnInfo("confidence_score")
    val confidenceScore: Float,
    @ColumnInfo("timestamp")
    val timestamp: Long
)