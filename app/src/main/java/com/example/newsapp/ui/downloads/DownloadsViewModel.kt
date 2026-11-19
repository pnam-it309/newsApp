package com.example.newsapp.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.R
import com.example.newsapp.data.local.AppDatabase
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val activeDownloads: List<ActiveDownloadItem> = emptyList(),
    val completedDownloads: List<CompletedDownloadItem> = emptyList(),
    val usedStorageGb: Double = 0.0,
    val totalStorageGb: Double = 32.0,
    val areAllPaused: Boolean = false,
    val openArticleEvent: Article? = null,
    val userMessage: String? = null
)

class DownloadsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: DownloadsRepository = DownloadsRepositoryImpl(
        articleDao = AppDatabase.getInstance(application).articleDao()
    )

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadDownloadsData()
    }

    fun loadDownloadsData() {
        viewModelScope.launch {
            val activeItems = repository.getActiveDownloads()
            val completedItems = repository.getCompletedDownloads()
            val usedGb = repository.calculateUsedStorageGb(activeItems, completedItems)

            _uiState.update {
                it.copy(
                    activeDownloads = activeItems,
                    completedDownloads = completedItems,
                    usedStorageGb = usedGb
                )
            }
        }
    }

    fun onCompletedItemClicked(itemId: String) {
        viewModelScope.launch {
            val article = repository.getArticleById(itemId)
            if (article != null) {
                _uiState.update { it.copy(openArticleEvent = article) }
            } else {
                val msg = getApplication<Application>().getString(R.string.downloads_msg_article_unavailable)
                _uiState.update { it.copy(userMessage = msg) }
            }
        }
    }

    fun onOpenArticleEventHandled() {
        _uiState.update { it.copy(openArticleEvent = null) }
    }

    fun toggleItemPause(itemId: String) {
        _uiState.update { state ->
            val updated = state.activeDownloads.map {
                if (it.id == itemId) it.copy(isPaused = !it.isPaused) else it
            }
            state.copy(activeDownloads = updated)
        }
    }

    fun cancelDownload(itemId: String) {
        val msg = getApplication<Application>().getString(R.string.downloads_msg_cancelled)
        _uiState.update { state ->
            val updated = state.activeDownloads.filterNot { it.id == itemId }
            val usedGb = repository.calculateUsedStorageGb(updated, state.completedDownloads)
            state.copy(
                activeDownloads = updated,
                usedStorageGb = usedGb,
                userMessage = msg
            )
        }
    }

    fun deleteCompleted(itemId: String) {
        viewModelScope.launch {
            repository.deleteCompletedDownload(itemId)
            loadDownloadsData()
            val msg = getApplication<Application>().getString(R.string.downloads_msg_deleted)
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun togglePauseAll() {
        _uiState.update { state ->
            val targetState = !state.areAllPaused
            val updated = state.activeDownloads.map { it.copy(isPaused = targetState) }
            state.copy(areAllPaused = targetState, activeDownloads = updated)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllCompletedDownloads()
            loadDownloadsData()
            val msg = getApplication<Application>().getString(R.string.downloads_msg_cleared)
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
