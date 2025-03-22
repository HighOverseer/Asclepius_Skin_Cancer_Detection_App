package com.dicoding.asclepius.domain.data

import androidx.paging.PagingData
import com.dicoding.asclepius.domain.common.Resource
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.domain.model.PredictionHistory
import kotlinx.coroutines.flow.Flow

interface Repository {

    fun getPredictionHistories(
        query: String = ""
    ): Flow<PagingData<PredictionHistory>>

    suspend fun insertPredictionHistory(
        predictionHistory: PredictionHistory
    )

    suspend fun getNewsAboutCancer(): Resource<List<CancerNewsPreview>>

    companion object {
        const val PAGE_SIZE = 10
        const val ENABLE_PLACE_HOLDERS = true

        const val CANCER_NEWS_REQUESTED_ITEM_COUNT = 10
    }
}