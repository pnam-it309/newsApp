package com.example.newsapp.ui.main

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.local.AppDatabase
import com.example.newsapp.data.repository.NewsRepository
import com.example.newsapp.data.repository.NewsRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: NewsRepository = NewsRepositoryImpl(
        articleDao = AppDatabase.getInstance(application).articleDao()
    )

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadNews()
    }

    fun loadNews(categoryKey: String? = _uiState.value.selectedCategory) {
        val selectedCat = categoryKey ?: "ALL"
        _uiState.update { it.copy(isLoading = true, selectedCategory = selectedCat, userMessage = null) }

        viewModelScope.launch {
            val result = repository.getTopHeadlines(category = selectedCat)

            result.onSuccess { fetchedArticles ->
                if (fetchedArticles.isNotEmpty()) {
                    val topStory = fetchedArticles.first()
                    val newsList = fetchedArticles.drop(1)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            topStory = topStory,
                            articles = newsList,
                            userMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            topStory = null,
                            articles = emptyList(),
                            userMessage = "Chưa có bài viết nào trong RoomDB"
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        topStory = null,
                        articles = emptyList(),
                        userMessage = error.message ?: "Lỗi tải dữ liệu"
                    )
                }
            }
        }
    }

    fun onCategorySelected(categoryKey: String) {
        if (_uiState.value.selectedCategory == categoryKey && _uiState.value.searchQuery.isEmpty()) return
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "") }
        loadNews(categoryKey = categoryKey)
    }

    fun onSearchQueryChanged(query: String?) {
        searchJob?.cancel()
        val trimmed = query?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchQuery = "", isLoading = false) }
            loadNews(_uiState.value.selectedCategory)
            return
        }

        _uiState.update { it.copy(searchQuery = trimmed, isLoading = true) }
        searchJob = viewModelScope.launch {
            delay(300)
            val result = repository.searchNews(query = trimmed)
            result.onSuccess { fetchedArticles ->
                if (fetchedArticles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            topStory = fetchedArticles.firstOrNull(),
                            articles = fetchedArticles,
                            userMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            topStory = null,
                            articles = emptyList(),
                            userMessage = "Không tìm thấy kết quả phù hợp"
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        topStory = null,
                        articles = emptyList(),
                        userMessage = "Không tìm thấy kết quả phù hợp"
                    )
                }
            }
        }
    }

    fun toggleTopStoryBookmark() {
        val currentTopStory = _uiState.value.topStory ?: return
        val newBookmarkedState = !_uiState.value.isTopStoryBookmarked
        val msg = if (newBookmarkedState) "Đã lưu bài viết vào RoomDB" else "Đã bỏ lưu bài viết"

        _uiState.update {
            it.copy(
                isTopStoryBookmarked = newBookmarkedState,
                userMessage = msg
            )
        }

        viewModelScope.launch {
            repository.toggleBookmark(currentTopStory.id, newBookmarkedState)
        }
    }

    fun onThemeToggleClicked() {
        val isCurrentlyDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val nextMode = if (isCurrentlyDark) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        val msg = if (isCurrentlyDark) "Light Mode Enabled" else "Dark Mode Enabled"

        _uiState.update {
            it.copy(
                themeModeEvent = nextMode,
                userMessage = msg
            )
        }
    }

    fun themeModeEventHandled() {
        _uiState.update { it.copy(themeModeEvent = null) }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
