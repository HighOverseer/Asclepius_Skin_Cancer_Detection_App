package com.dicoding.asclepius.data.mapper

import com.dicoding.asclepius.data.local.PredictionHistoryEntity
import com.dicoding.asclepius.data.remote.ArticlesItem
import com.dicoding.asclepius.data.utils.timestampToDateString
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.domain.model.PredictionHistory


object MapperDtoToModel {

    fun mapPredictionHistoryEntity(
        predictionHistoryEntity: PredictionHistoryEntity
    ): PredictionHistory {
        val dateString = predictionHistoryEntity.timestamp.timestampToDateString()

        return PredictionHistory(
            id = predictionHistoryEntity.id,
            sessionName = predictionHistoryEntity.sessionName,
            imageUri = predictionHistoryEntity.imageUri,
            modelOutput = ModelOutput(
                label = predictionHistoryEntity.label,
                confidenceScore = predictionHistoryEntity.confidenceScore
            ),
            date = dateString
        )
    }

    fun mapHealthNewsCancerDto(
        articlesItemsDto: List<ArticlesItem?>?
    ): List<CancerNewsPreview> {
        return articlesItemsDto?.map {
            CancerNewsPreview(
                title = it?.title ?: "",
                imageUrl = it?.urlToImage ?: "",
                description = it?.description ?: "",
                url = it?.url ?: "",
                publishedDate = it?.publishedAt ?: "",
                author = it?.author ?: "",
            )
        } ?: emptyList()
    }
}