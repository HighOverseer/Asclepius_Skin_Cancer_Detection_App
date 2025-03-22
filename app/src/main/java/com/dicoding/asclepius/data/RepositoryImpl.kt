package com.dicoding.asclepius.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.dicoding.asclepius.BuildConfig
import com.dicoding.asclepius.R
import com.dicoding.asclepius.data.local.AppDatabase
import com.dicoding.asclepius.data.mapper.MapperDtoToModel
import com.dicoding.asclepius.data.mapper.MapperModelToDto
import com.dicoding.asclepius.data.remote.FailedResponseDto
import com.dicoding.asclepius.data.remote.HealthApiService
import com.dicoding.asclepius.domain.common.Resource
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.data.Repository
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.domain.model.PredictionHistory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl @Inject constructor(
    appDatabase: AppDatabase,
    private val mapperDtoToModel: MapperDtoToModel,
    private val mapperModelToDto: MapperModelToDto,
    private val healthApiService: HealthApiService,
    private val gson: Gson
) : Repository {

    private val predictionHistoryDao = appDatabase.predictionHistoryDao()

    override suspend fun getNewsAboutCancer(): Resource<List<CancerNewsPreview>> {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val currentDate = calendar.time
            val df = SimpleDateFormat(HealthApiService.DATE_FORMAT, Locale.getDefault())
            val dateString = df.format(currentDate)

            val dto = healthApiService.getNewsAboutCancer(
                dateString,
                BuildConfig.HEALTH_NEWS_API_KEY,
            )

            val filteredDto = dto.articles?.filter {
                it?.urlToImage != null && it.author != null

            }?.take(Repository.CANCER_NEWS_REQUESTED_ITEM_COUNT)

            val cancerNewsPreviews = mapperDtoToModel.mapHealthNewsCancerDto(filteredDto)

            return Resource.Success(cancerNewsPreviews)
        } catch (e: HttpException) {
            val errorResponseBodyString = e.response()?.errorBody()?.string()

            if (errorResponseBodyString?.contains("{") == false) {
                return Resource.Failure(
                    StringRes.Dynamic(errorResponseBodyString)
                )
            }

            val responseType = object : TypeToken<FailedResponseDto>() {}.type
            val errorResponse: FailedResponseDto = gson.fromJson(
                errorResponseBodyString,
                responseType
            )

            return errorResponse.message?.let {
                Resource.Failure(
                    message = StringRes.Dynamic(it)
                )
            } ?: Resource.Failure(
                StringRes.Static(R.string.sorry_something_went_wrong_pleasy_try_again)
            )

        } catch (e: Exception) {
            if (e is CancellationException) throw e

            return Resource.Error(e)
        }
    }

    override fun getPredictionHistories(
        query: String
    ): Flow<PagingData<PredictionHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = Repository.PAGE_SIZE,
                enablePlaceholders = Repository.ENABLE_PLACE_HOLDERS
            ),
            pagingSourceFactory = {
                predictionHistoryDao.getPredictionHistories(query)
            }
        ).flow.map { pagingData ->
            pagingData.map {
                mapperDtoToModel.mapPredictionHistoryEntity(
                    it
                )
            }
        }
    }

    override suspend fun insertPredictionHistory(predictionHistory: PredictionHistory) {
        val predictionHistoryEntity = mapperModelToDto.mapPredictionHistory(
            predictionHistory
        )

        if (predictionHistoryEntity == null) return

        predictionHistoryDao.insertPredictionHistory(predictionHistoryEntity)
    }
}