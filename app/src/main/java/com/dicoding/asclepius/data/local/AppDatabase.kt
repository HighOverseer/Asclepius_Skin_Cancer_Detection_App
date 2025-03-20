package com.dicoding.asclepius.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PredictionHistoryEntity::class],
    version = 1,
)
abstract class AppDatabase:RoomDatabase() {

    abstract fun predictionHistoryDao():PredictionHistoryDao
}