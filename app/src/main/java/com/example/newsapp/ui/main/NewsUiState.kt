package com.example.newsapp.ui.main

import com.example.newsapp.ui.entity.Article

data class NewsUiState(
    val isLoading: Boolean = false,
    val topStory: Article? = null,
    val articles: List<Article> = emptyList(),
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val isTopStoryBookmarked: Boolean = false,
    val userMessage: String? = null
)
