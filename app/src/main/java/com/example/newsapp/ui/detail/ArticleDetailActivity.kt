package com.example.newsapp.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.newsapp.R
import com.example.newsapp.data.local.AppDatabase
import com.example.newsapp.data.repository.NewsRepositoryImpl
import com.example.newsapp.databinding.ActivityArticleDetailBinding
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.launch

class ArticleDetailActivity : BaseActivity() {

    private val binding: ActivityArticleDetailBinding by lazy {
        ActivityArticleDetailBinding.inflate(layoutInflater)
    }
    private val article: Article? by lazy {
        intent.extras?.let { bundle ->
            BundleCompat.getParcelable(bundle, EXTRA_ARTICLE, Article::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemNavigationBar()
        setContentView(binding.root)

        bindArticleData()
        setupListeners()
    }

    private fun bindArticleData() {
        val currentArticle = article ?: return

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
            currentArticle.title + "\n\n" + getString(R.string.article_time_placeholder)
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

        updateBookmarkIcon(currentArticle.isSaved)
    }

    private fun updateBookmarkIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        binding.btnBookmark.setImageResource(iconRes)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            val shareText = "${article?.title}\n${article?.url ?: ""}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share article via"))
        }

        binding.btnBookmark.setOnClickListener {
            val current = article ?: return@setOnClickListener
            current.isSaved = !current.isSaved
            updateBookmarkIcon(current.isSaved)

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val repo = NewsRepositoryImpl(articleDao = db.articleDao())
                repo.toggleBookmark(current.id, current.isSaved)
            }

            val msg = if (current.isSaved) getString(R.string.msg_bookmark_saved) else getString(R.string.msg_bookmark_removed)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnDownload.setOnClickListener {
            openArticleUrl()
        }

        binding.btnDownloadFull.setOnClickListener {
            openArticleUrl()
        }
    }

    private fun openArticleUrl() {
        val url = article?.url
        if (!url.isNullOrEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Article URL is not available", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_ARTICLE = "extra_article"
    }
}
