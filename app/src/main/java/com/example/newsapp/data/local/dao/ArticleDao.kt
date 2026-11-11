package com.example.newsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.newsapp.data.local.entity.ArticleEntity

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY fetchedAt DESC")
    suspend fun getArticlesByCategory(category: String): List<ArticleEntity>

    @Query("SELECT * FROM articles ORDER BY fetchedAt DESC")
    suspend fun getAllArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY fetchedAt DESC")
    suspend fun getSavedArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR source LIKE '%' || :query || '%' ORDER BY fetchedAt DESC")
    suspend fun searchArticles(query: String): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>): List<Long>

    @Query("UPDATE articles SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: String, isSaved: Boolean): Int

    @Query("DELETE FROM articles WHERE isSaved = 0")
    suspend fun clearUnsavedArticles(): Int
}
