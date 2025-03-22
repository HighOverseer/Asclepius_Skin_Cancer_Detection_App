package com.dicoding.asclepius.data.mapper

import com.dicoding.asclepius.data.local.PredictionHistoryEntity
import com.dicoding.asclepius.data.utils.dateStringToTimestamp
import com.dicoding.asclepius.domain.model.PredictionHistory


object MapperModelToDto {
    fun mapPredictionHistory(
        predictionHistory: PredictionHistory
    ): PredictionHistoryEntity? {

        val timestamp = predictionHistory.date.dateStringToTimestamp()

        return PredictionHistoryEntity(
            id = predictionHistory.id,
            sessionName = predictionHistory.sessionName,
            imageUri = predictionHistory.imageUri,
            label = predictionHistory.modelOutput.label,
            confidenceScore = predictionHistory.modelOutput.confidenceScore,
            timestamp = timestamp ?: return null
        )
    }
}