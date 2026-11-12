package com.example.newsapp.data.repository

import com.example.newsapp.ui.entity.Article

interface NewsRepository {
    suspend fun getTopHeadlines(category: String? = null): Result<List<Article>>
    suspend fun searchNews(query: String): Result<List<Article>>
    suspend fun toggleBookmark(articleId: String, isBookmarked: Boolean)
    suspend fun getSavedArticles(): List<Article>
}
