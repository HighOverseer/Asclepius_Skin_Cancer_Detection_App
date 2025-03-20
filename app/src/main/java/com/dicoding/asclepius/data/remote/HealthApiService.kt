package com.dicoding.asclepius.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface HealthApiService {
    @GET("everything")
    suspend fun getNewsAboutCancer(
        @Query("from") fromDate:String,
        @Query("apiKey") apiKey:String,
        @Query("q") q:String = QUERY,
        @Query("searchIn") searchIn:String = SEARCH_IN,
        @Query("pageSize") pageSize:Int = PAGE_SIZE,
        @Query("page") page:Int = PAGE,
        @Query("sortBy") sortBy:String = SORT_BY,
        @Query("excludeDomains") excludeDomains:String = EXCLUDE_DOMAINS
    ):HealthNewsCancerDto

    companion object{
        const val DATE_FORMAT = "yyyy-MM-dd"
        private const val QUERY = "cancer"
        private const val SEARCH_IN = "title"
        private const val PAGE_SIZE = 20
        private const val PAGE = 1
        private const val SORT_BY = "popularity"
        private const val EXCLUDE_DOMAINS = "sciencedaily.com"
    }
}