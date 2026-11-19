package com.example.newsapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.newsapp.R
import com.example.newsapp.databinding.ActivityHomeBinding
import com.example.newsapp.ui.adapter.NewsAdapter
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.detail.ArticleDetailActivity
import com.example.newsapp.ui.downloads.DownloadsActivity
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityHomeBinding>(ActivityHomeBinding::inflate) {

    private val viewModel: NewsViewModel by viewModels()

    private val newsAdapter by lazy {
        NewsAdapter().apply {
            setOnItemClickListener { article -> openArticleDetail(article) }
        }
    }

    private val categoryButtons by lazy {
        with(binding.includedCategoryBar) {
            mapOf(
                "ALL" to btnCatGeneral,
                "TECH" to btnCatTech,
                "BUSINESS" to btnCatBusiness,
                "SPORTS" to btnCatSports,
                "ENTERTAINMENT" to btnCatEntertainment
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.includedNewsSection.rvNewsList.adapter = newsAdapter
        setupCategoryBar()
        setupSearchView()
        setupThemeToggle()
        setupBottomNavigation()
        binding.includedTopStory.btnBookmarkTopStory.setOnClickListener {
            viewModel.toggleTopStoryBookmark()
        }

        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderUiState(it) }
            }
        }
    }

    private fun renderUiState(state: NewsUiState) {
        val isSearching = state.searchQuery.isNotBlank()
        binding.includedTopStory.root.isVisible = !isSearching

        if (!isSearching) {
            state.topStory?.let { setupTopStoryUi(it, state.isTopStoryBookmarked) }
        }

        newsAdapter.submitList(state.articles)
        binding.includedNewsSection.tvStoriesCount.text =
            getString(R.string.news_stories_count_format, state.articles.size)
        updateCategoryButtonStyles(state.selectedCategory)

        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.userMessageShown()
        }
    }

    private fun setupTopStoryUi(article: Article, isBookmarked: Boolean) {
        with(binding.includedTopStory) {
            tvTopStoryTitle.text = article.title.ifBlank { getString(R.string.default_article_title) }
            val source = article.source.ifBlank { getString(R.string.default_source) }
            val time = article.time.ifBlank { getString(R.string.default_time_latest) }
            tvTopStoryMeta.text = "$source • $time"

            imgTopStory.load(article.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }

            btnBookmarkTopStory.setImageResource(
                if (isBookmarked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            cardTopStory.setOnClickListener { openArticleDetail(article) }
        }
    }

    private fun openArticleDetail(article: Article) {
        val intent = Intent(this, ArticleDetailActivity::class.java).apply {
            putExtra(ArticleDetailActivity.EXTRA_ARTICLE, article)
        }
        startActivity(intent)
    }

    private fun setupCategoryBar() {
        categoryButtons.forEach { (catKey, button) ->
            button.setOnClickListener {
                binding.includedSearchRow.searchView.apply {
                    setQuery("", false)
                    clearFocus()
                }
                viewModel.onCategorySelected(catKey)
            }
        }
    }

    private fun updateCategoryButtonStyles(selectedKey: String) {
        val activeColor = ContextCompat.getColor(this, R.color.white)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        categoryButtons.forEach { (key, button) ->
            val isSelected = key == selectedKey
            button.setBackgroundResource(if (isSelected) R.drawable.bg_rounded_green else R.drawable.bg_rounded_input)
            button.setTextColor(if (isSelected) activeColor else inactiveColor)
        }
    }

    private fun setupSearchView() {
        binding.includedSearchRow.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.onSearchQueryChanged(newText)
                    return true
                }
            })
            setOnCloseListener {
                viewModel.onSearchQueryChanged("")
                false
            }
        }
    }

    private fun setupThemeToggle() {
        binding.includedHeaderBar.btnThemeToggle.setOnClickListener {
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            val newMode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(newMode)
            val msgResId = if (isNight) R.string.msg_light_mode_enabled else R.string.msg_dark_mode_enabled
            Toast.makeText(this, getString(msgResId), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        with(binding.includedBottomNav) {
            btnNavSaved.setOnClickListener {
                startActivity(Intent(this@MainActivity, DownloadsActivity::class.java))
            }
            btnNavProfile.setOnClickListener {
                Toast.makeText(this@MainActivity, getString(R.string.nav_profile), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
