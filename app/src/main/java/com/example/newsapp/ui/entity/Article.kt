package com.example.newsapp.ui.entity

import java.io.Serializable

data class Article(
    val id: String,
    val title: String,
    val source: String,
    val time: String,
    val category: String,
    val authorName: String = "Unknown Author",
    val authorRole: String = "Reporter",
    val body: String = "",
    var isSaved: Boolean = false,
    val imageUrl: String? = null,
    val url: String? = null,
    val imageResId: Int? = null
) : Serializable

data class Category(
    val id: String,
    val name: String,
    var isSelected: Boolean = false
)
