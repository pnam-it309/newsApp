package com.example.newsapp.ui.downloads

import com.example.newsapp.data.local.dao.ArticleDao
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface DownloadsRepository {
    suspend fun getActiveDownloads(): List<ActiveDownloadItem>
    suspend fun getCompletedDownloads(): List<CompletedDownloadItem>
    suspend fun getArticleById(id: String): Article?
    suspend fun deleteCompletedDownload(id: String)
    suspend fun clearAllCompletedDownloads()
    fun calculateUsedStorageGb(
        activeList: List<ActiveDownloadItem>,
        completedList: List<CompletedDownloadItem>
    ): Double
}

class DownloadsRepositoryImpl(
    private val articleDao: ArticleDao
) : DownloadsRepository {

    // Danh sách active thực tế
    private val activeDownloadQueue = mutableListOf<ActiveDownloadItem>()

    override suspend fun getActiveDownloads(): List<ActiveDownloadItem> = withContext(Dispatchers.IO) {
        activeDownloadQueue.toList()
    }

    override suspend fun getCompletedDownloads(): List<CompletedDownloadItem> = withContext(Dispatchers.IO) {
        val savedArticles = articleDao.getSavedArticles()
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        savedArticles.map { entity ->
            val format = if (!entity.url.isNullOrBlank() && entity.url.endsWith(".pdf", ignoreCase = true)) "PDF" else "HTML"
            val bodySizeMb = (entity.body.length / (1024.0 * 1024.0)) + 0.1
            val formattedSize = String.format(Locale.US, "%.1f", bodySizeMb).toDouble()

            CompletedDownloadItem(
                id = entity.id,
                title = entity.title,
                format = format,
                sizeMb = formattedSize,
                completedTime = dateFormat.format(Date(entity.fetchedAt))
            )
        }
    }

    override suspend fun getArticleById(id: String): Article? = withContext(Dispatchers.IO) {
        val entity = articleDao.getSavedArticles().firstOrNull { it.id == id }
            ?: articleDao.getAllArticles().firstOrNull { it.id == id }
        entity?.toArticle()
    }

    override suspend fun deleteCompletedDownload(id: String): Unit = withContext(Dispatchers.IO) {
        articleDao.updateSavedStatus(id, false)
        Unit
    }

    override suspend fun clearAllCompletedDownloads(): Unit = withContext(Dispatchers.IO) {
        val savedList = articleDao.getSavedArticles()
        savedList.forEach { entity ->
            articleDao.updateSavedStatus(entity.id, false)
        }
    }

    override fun calculateUsedStorageGb(
        activeList: List<ActiveDownloadItem>,
        completedList: List<CompletedDownloadItem>
    ): Double {
        val totalMb = activeList.sumOf { it.currentSizeMb } + completedList.sumOf { it.sizeMb }
        val usedGb = totalMb / 1024.0
        return String.format(Locale.US, "%.1f", usedGb).toDouble()
    }
}
