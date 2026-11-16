package com.example.newsapp.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.R
import com.example.newsapp.data.local.AppDatabase
import com.example.newsapp.data.repository.NewsRepository
import com.example.newsapp.data.repository.NewsRepositoryImpl
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleDetailUiState(
    val article: Article? = null,
    val isSaved: Boolean = false,
    val userMessage: String? = null,
    val openUrlEvent: String? = null,
    val shareEvent: Pair<String, String>? = null,
    val copyLinkEvent: String? = null
)

class ArticleDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: NewsRepository = NewsRepositoryImpl(
        articleDao = AppDatabase.getInstance(application).articleDao()
    )

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    fun setArticle(article: Article?) {
        if (article == null) return
        _uiState.update {
            it.copy(
                article = article,
                isSaved = article.isSaved
            )
        }
        checkSavedStatus(article.id)
    }

    private fun checkSavedStatus(articleId: String) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val savedList = db.articleDao().getSavedArticles()
            val isSaved = savedList.any { it.id == articleId }
            _uiState.update {
                it.copy(
                    isSaved = isSaved,
                    article = it.article?.copy(isSaved = isSaved)
                )
            }
        }
    }

    fun toggleBookmark() {
        val currentArticle = _uiState.value.article ?: return
        val newSavedState = !_uiState.value.isSaved
        val context = getApplication<Application>()
        val msg = if (newSavedState) {
            context.getString(R.string.msg_bookmark_saved)
        } else {
            context.getString(R.string.msg_bookmark_removed)
        }

        _uiState.update {
            it.copy(
                isSaved = newSavedState,
                article = it.article?.copy(isSaved = newSavedState),
                userMessage = msg
            )
        }

        viewModelScope.launch {
            repository.toggleBookmark(currentArticle.id, newSavedState)
        }
    }

    fun onShareClicked() {
        val currentArticle = _uiState.value.article ?: return
        _uiState.update {
            it.copy(shareEvent = Pair(currentArticle.title, currentArticle.url.orEmpty()))
        }
    }

    fun onOpenUrlClicked() {
        val url = _uiState.value.article?.url
        if (!url.isNullOrEmpty()) {
            _uiState.update { it.copy(openUrlEvent = url) }
        } else {
            val context = getApplication<Application>()
            _uiState.update { it.copy(userMessage = context.getString(R.string.detail_url_unavailable)) }
        }
    }

    fun onCopyLinkClicked() {
        val url = _uiState.value.article?.url
        val context = getApplication<Application>()
        if (!url.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    copyLinkEvent = url,
                    userMessage = context.getString(R.string.msg_link_copied)
                )
            }
        } else {
            _uiState.update { it.copy(userMessage = context.getString(R.string.detail_url_unavailable)) }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun openUrlEventHandled() {
        _uiState.update { it.copy(openUrlEvent = null) }
    }

    fun shareEventHandled() {
        _uiState.update { it.copy(shareEvent = null) }
    }

    fun copyLinkEventHandled() {
        _uiState.update { it.copy(copyLinkEvent = null) }
    }
}
