package com.example.newsapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newsapp.ui.entity.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val source: String,
    val time: String,
    val category: String,
    val authorName: String = "Unknown Author",
    val authorRole: String = "Reporter",
    val body: String = "",
    val isSaved: Boolean = false,
    val imageUrl: String? = null,
    val url: String? = null,
    val imageResId: Int? = null,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun toArticle(): Article = Article(
        id = id,
        title = title,
        source = source,
        time = time,
        category = category,
        authorName = authorName,
        authorRole = authorRole,
        body = body,
        isSaved = isSaved,
        imageUrl = imageUrl,
        url = url,
        imageResId = imageResId
    )

    companion object {
        fun fromArticle(article: Article): ArticleEntity = ArticleEntity(
            id = article.id,
            title = article.title,
            source = article.source,
            time = article.time,
            category = article.category,
            authorName = article.authorName,
            authorRole = article.authorRole,
            body = article.body,
            isSaved = article.isSaved,
            imageUrl = article.imageUrl,
            url = article.url,
            imageResId = article.imageResId
        )
    }
}
