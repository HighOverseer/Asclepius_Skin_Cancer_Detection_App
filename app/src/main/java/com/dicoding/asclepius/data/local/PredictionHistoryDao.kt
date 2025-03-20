package com.dicoding.asclepius.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PredictionHistoryDao{

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPredictionHistory(
        predictionHistoryEntity: PredictionHistoryEntity
    )

    @Query("""
        SELECT * FROM prediction_history WHERE session_name LIKE '%' || :query || '%' ORDER BY timestamp DESC
    """)
    fun getPredictionHistories(query:String):PagingSource<Int, PredictionHistoryEntity>

}