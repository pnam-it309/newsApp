package com.example.newsapp.data.api

import com.example.newsapp.data.api.model.NewsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("category") category: String? = null,
        @Query("q") query: String? = null
    ): Response<NewsResponseDto>

    @GET("everything")
    suspend fun getEverything(
        @Query("q") query: String,
        @Query("language") language: String? = "en"
    ): Response<NewsResponseDto>

    companion object {
        const val BASE_URL = "https://newsapi.org/v2/"
    }
}
