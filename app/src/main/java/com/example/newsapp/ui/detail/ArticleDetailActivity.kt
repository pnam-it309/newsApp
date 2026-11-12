package com.example.newsapp.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.newsapp.R
import com.example.newsapp.databinding.ActivityArticleDetailBinding
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.launch

class ArticleDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityArticleDetailBinding
    private val viewModel: ArticleDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemNavigationBar()
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val article = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_ARTICLE, Article::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_ARTICLE) as? Article
        }

        viewModel.setArticle(article)

        setupListeners()
        observeUiState()
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

    private fun renderUiState(state: ArticleDetailUiState) {
        val currentArticle = state.article ?: return

        val defaultTitle = getString(R.string.default_article_title)
        val defaultAuthor = getString(R.string.default_author)
        val defaultSource = getString(R.string.default_source)
        val defaultTime = getString(R.string.default_time_latest)

        binding.tvDetailCategory.text = currentArticle.category.uppercase()
        binding.tvHeroCategory.text = currentArticle.category.uppercase()
        binding.tvDetailTitle.text = currentArticle.title.ifBlank { defaultTitle }
        binding.tvHeroCaption.text = currentArticle.title.ifBlank { defaultTitle }
        binding.tvAuthorName.text = currentArticle.authorName.ifBlank { defaultAuthor }
        binding.tvAuthorRole.text = currentArticle.authorRole.ifBlank { getString(R.string.default_author_role) }

        val source = currentArticle.source.ifBlank { defaultSource }
        val time = currentArticle.time.ifBlank { defaultTime }
        binding.tvArticleMetaInfo.text = "$source\n$time"

        binding.tvArticleBody.text = if (currentArticle.body.isNotBlank()) {
            currentArticle.body
        } else {
            getString(R.string.detail_no_body_fallback)
        }

        if (!currentArticle.imageUrl.isNullOrEmpty()) {
            binding.imgArticleHero.load(currentArticle.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            binding.imgArticleHero.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        updateBookmarkIcon(state.isSaved)

        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.userMessageShown()
        }

        state.openUrlEvent?.let { url ->
            openWebUrl(url)
            viewModel.openUrlEventHandled()
        }

        state.shareEvent?.let { (title, url) ->
            shareArticleText(title, url)
            viewModel.shareEventHandled()
        }
    }

    private fun updateBookmarkIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) {
            R.drawable.ic_bookmark_filled
        } else {
            R.drawable.ic_bookmark
        }
        binding.btnBookmark.setImageResource(iconRes)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            viewModel.onShareClicked()
        }

        binding.btnBookmark.setOnClickListener {
            viewModel.toggleBookmark()
        }

        binding.btnDownload.setOnClickListener {
            showActionsModal()
        }

        binding.btnDownloadFull.setOnClickListener {
            showActionsModal()
        }

        binding.cardArticleDownload.setOnClickListener {
            showActionsModal()
        }
    }

    private fun showActionsModal() {
        val currentArticle = viewModel.uiState.value.article ?: return
        val bottomSheet = ArticleActionsBottomSheet.newInstance(currentArticle) {
            viewModel.toggleBookmark()
        }
        bottomSheet.show(supportFragmentManager, ArticleActionsBottomSheet.TAG)
    }

    private fun openWebUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.detail_cannot_open_url), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareArticleText(title: String, url: String) {
        val shareText = "$title\n$url"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.detail_share_chooser_title)))
    }

    companion object {
        const val EXTRA_ARTICLE = "extra_article"
    }
}
