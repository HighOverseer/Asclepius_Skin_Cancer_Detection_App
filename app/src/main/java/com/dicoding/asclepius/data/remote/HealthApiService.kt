package com.dicoding.asclepius.data.remote

import com.dicoding.asclepius.data.remote.dto.HealthNewsCancerDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HealthApiService {
    @GET(ENDPOINT_EVERYTHING)
    suspend fun getNewsAboutCancer(
        @Query(QUERY_FROM) fromDate: String,
        @Query(QUERY_API_KEY) apiKey: String,
        @Query(QUERY_Q) q: String = DEFAULT_QUERY,
        @Query(QUERY_SEARCH_IN) searchIn: String = SEARCH_IN,
        @Query(QUERY_PAGE_SIZE) pageSize: Int = PAGE_SIZE,
        @Query(QUERY_PAGE) page: Int = PAGE,
        @Query(QUERY_SORT_BY) sortBy: String = SORT_BY,
        @Query(QUERY_EXCLUDE_DOMAINS) excludeDomains: String = EXCLUDE_DOMAINS
    ): HealthNewsCancerDto

    companion object {
        private const val ENDPOINT_EVERYTHING = "everything"

        private const val QUERY_FROM = "from"
        private const val QUERY_API_KEY = "apiKey"
        private const val QUERY_Q = "q"
        private const val QUERY_SEARCH_IN = "searchIn"
        private const val QUERY_PAGE_SIZE = "pageSize"
        private const val QUERY_PAGE = "page"
        private const val QUERY_SORT_BY = "sortBy"
        private const val QUERY_EXCLUDE_DOMAINS = "excludeDomains"

        const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DEFAULT_QUERY = "cancer"
        private const val SEARCH_IN = "title"
        private const val PAGE_SIZE = 30
        private const val PAGE = 1
        private const val SORT_BY = "popularity"
        private const val EXCLUDE_DOMAINS = "sciencedaily.com"
    }
}