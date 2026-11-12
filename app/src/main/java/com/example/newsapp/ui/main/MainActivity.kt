package com.example.newsapp.ui.main

import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.newsapp.R
import com.example.newsapp.databinding.ActivityHomeBinding
import com.example.newsapp.ui.adapter.NewsAdapter
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private val binding: ActivityHomeBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityHomeBinding.inflate(layoutInflater)
    }
    private val newsAdapter: NewsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NewsAdapter().apply {
            setOnItemClickListener { article ->
                Toast.makeText(this@MainActivity, article.title, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemNavigationBar()
        setContentView(binding.root)

        setupRecyclerView()
        setupCategoryBar()
        setupSearchView()
        setupThemeToggle()
        setupBottomNavigation()
        setupTopStoryBookmarkClick()

        observeUiState()
    }

    private fun setupRecyclerView() {
        binding.includedNewsSection.rvNewsList.adapter = newsAdapter
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: NewsUiState) {
        val isSearching = state.searchQuery.isNotBlank()
        if (isSearching) {
            binding.includedTopStory.root.visibility = android.view.View.GONE
        } else {
            binding.includedTopStory.root.visibility = android.view.View.VISIBLE
            state.topStory?.let { topStory ->
                setupTopStoryUi(topStory, state.isTopStoryBookmarked)
            }
        }

        newsAdapter.submitList(state.articles)
        binding.includedNewsSection.tvStoriesCount.text = "${state.articles.size} Stories"

        updateCategoryButtonStyles(state.selectedCategory)

        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.userMessageShown()
        }
    }

    private fun setupTopStoryUi(article: Article, isBookmarked: Boolean) {
        val topStoryBinding = binding.includedTopStory
        topStoryBinding.tvTopStoryTitle.text = article.title.ifBlank { getString(R.string.default_article_title) }
        val sourceText = article.source.ifBlank { getString(R.string.default_source) }
        val timeText = article.time.ifBlank { getString(R.string.default_time_latest) }
        topStoryBinding.tvTopStoryMeta.text = "$sourceText • $timeText"

        if (!article.imageUrl.isNullOrEmpty()) {
            topStoryBinding.imgTopStory.load(article.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            topStoryBinding.imgTopStory.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val iconRes = if (isBookmarked) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        topStoryBinding.btnBookmarkTopStory.setImageResource(iconRes)

        topStoryBinding.cardTopStory.setOnClickListener {
            Toast.makeText(this, article.title, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTopStoryBookmarkClick() {
        binding.includedTopStory.btnBookmarkTopStory.setOnClickListener {
            viewModel.toggleTopStoryBookmark()
        }
    }

    private fun setupCategoryBar() {
        val categoryBar = binding.includedCategoryBar
        val buttons = mapOf(
            "ALL" to categoryBar.btnCatGeneral,
            "TECH" to categoryBar.btnCatTech,
            "BUSINESS" to categoryBar.btnCatBusiness,
            "SPORTS" to categoryBar.btnCatSports,
            "ENTERTAINMENT" to categoryBar.btnCatEntertainment
        )

        buttons.forEach { (catKey, button) ->
            button.setOnClickListener {
                binding.includedSearchRow.searchView.setQuery("", false)
                binding.includedSearchRow.searchView.clearFocus()
                viewModel.onCategorySelected(catKey)
            }
        }
    }

    private fun updateCategoryButtonStyles(selectedKey: String) {
        val categoryBar = binding.includedCategoryBar
        val buttons = mapOf(
            "ALL" to categoryBar.btnCatGeneral,
            "TECH" to categoryBar.btnCatTech,
            "BUSINESS" to categoryBar.btnCatBusiness,
            "SPORTS" to categoryBar.btnCatSports,
            "ENTERTAINMENT" to categoryBar.btnCatEntertainment
        )

        buttons.forEach { (key, button) ->
            if (key == selectedKey) {
                button.setBackgroundResource(R.drawable.bg_rounded_green)
                button.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                button.setBackgroundResource(R.drawable.bg_rounded_input)
                button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    private fun setupSearchView() {
        val searchView = binding.includedSearchRow.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onSearchQueryChanged(query)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearchQueryChanged(newText)
                return true
            }
        })
        searchView.setOnCloseListener {
            viewModel.onSearchQueryChanged("")
            false
        }
    }

    private fun setupThemeToggle() {
        binding.includedHeaderBar.btnThemeToggle.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Light Mode Enabled", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Dark Mode Enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.includedBottomNav
        bottomNav.btnNavHome.setOnClickListener {
            Toast.makeText(this, "Home Tab Selected", Toast.LENGTH_SHORT).show()
        }
        bottomNav.btnNavSaved.setOnClickListener {
            Toast.makeText(this, "Saved Tab Selected", Toast.LENGTH_SHORT).show()
        }
        bottomNav.btnNavProfile.setOnClickListener {
            Toast.makeText(this, "Profile Tab Selected", Toast.LENGTH_SHORT).show()
        }
    }
}
