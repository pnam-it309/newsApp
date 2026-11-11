package com.example.newsapp.data.repository

import com.example.newsapp.data.api.NewsApiService
import com.example.newsapp.data.api.RetrofitClient
import com.example.newsapp.data.api.model.ArticleDto
import com.example.newsapp.data.local.dao.ArticleDao
import com.example.newsapp.data.local.entity.ArticleEntity
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsRepositoryImpl(
    private val apiService: NewsApiService = RetrofitClient.apiService,
    private val articleDao: ArticleDao
) : NewsRepository {

    override suspend fun getTopHeadlines(category: String?): Result<List<Article>> = withContext(Dispatchers.IO) {
        val targetCategory = category?.uppercase() ?: "ALL"
        val apiCategory = when (targetCategory) {
            "TECH" -> "technology"
            "BUSINESS" -> "business"
            "SPORTS" -> "sports"
            "ENTERTAINMENT" -> "entertainment"
            "GENERAL" -> "general"
            else -> null
        }

        try {
            val response = apiService.getTopHeadlines(category = apiCategory)
            if (response.isSuccessful && response.body()?.status == "ok") {
                val dtoList = response.body()?.articles ?: emptyList()
                val entities = dtoList.mapIndexed { index, dto ->
                    dto.toEntity(
                        id = "${targetCategory}_${dto.url.hashCode()}_$index",
                        category = targetCategory
                    )
                }

                if (entities.isNotEmpty()) {
                    articleDao.insertArticles(entities)
                }

                val articles = entities.map { it.toArticle() }
                Result.success(articles)
            } else {
                fetchCachedArticles(targetCategory)
            }
        } catch (e: Exception) {
            fetchCachedArticles(targetCategory)
        }
    }

    override suspend fun searchNews(query: String): Result<List<Article>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEverything(query = query, language = "en")
            if (response.isSuccessful && response.body()?.status == "ok") {
                val dtoList = response.body()?.articles ?: emptyList()
                val entities = dtoList.mapIndexed { index, dto ->
                    dto.toEntity(
                        id = "SEARCH_${dto.url.hashCode()}_$index",
                        category = "SEARCH"
                    )
                }

                if (entities.isNotEmpty()) {
                    articleDao.insertArticles(entities)
                }

                Result.success(entities.map { it.toArticle() })
            } else {
                val localResults = articleDao.searchArticles(query).map { it.toArticle() }
                Result.success(localResults)
            }
        } catch (e: Exception) {
            val localResults = articleDao.searchArticles(query).map { it.toArticle() }
            Result.success(localResults)
        }
    }

    override suspend fun toggleBookmark(articleId: String, isBookmarked: Boolean) {
        withContext(Dispatchers.IO) {
            articleDao.updateSavedStatus(articleId, isBookmarked)
        }
    }

    override suspend fun getSavedArticles(): List<Article> = withContext(Dispatchers.IO) {
        articleDao.getSavedArticles().map { it.toArticle() }
    }

    private suspend fun fetchCachedArticles(category: String): Result<List<Article>> {
        val cachedEntities = if (category == "ALL") {
            articleDao.getAllArticles()
        } else {
            articleDao.getArticlesByCategory(category)
        }

        return if (cachedEntities.isNotEmpty()) {
            Result.success(cachedEntities.map { it.toArticle() })
        } else {
            Result.failure(Exception("Không có kết nối mạng và chưa có dữ liệu lưu trữ trong RoomDB"))
        }
    }

    private fun ArticleDto.toEntity(id: String, category: String): ArticleEntity {
        val formattedTime = if (!publishedAt.isNullOrEmpty()) {
            publishedAt.take(10)
        } else {
            "Mới nhất"
        }
        return ArticleEntity(
            id,
            title ?: "Tin tức",
            source?.name ?: "NewsAPI",
            formattedTime,
            category,
            author ?: "Tòa soạn",
            "Phóng viên",
            content ?: description ?: "",
            false,
            urlToImage,
            url,
            null,
            System.currentTimeMillis()
        )
    }
}
