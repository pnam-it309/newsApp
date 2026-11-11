package com.example.newsapp.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.newsapp.R
import com.example.newsapp.data.local.AppDatabase
import com.example.newsapp.data.repository.NewsRepositoryImpl
import com.example.newsapp.databinding.ActivityArticleDetailBinding
import com.example.newsapp.ui.entity.Article
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding
    private var article: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        article = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_ARTICLE, Article::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_ARTICLE) as? Article
        }

        bindArticleData()
        setupListeners()
    }

    private fun bindArticleData() {
        val currentArticle = article ?: return

        binding.tvDetailCategory.text = currentArticle.category.uppercase()
        binding.tvHeroCategory.text = currentArticle.category.uppercase()
        binding.tvDetailTitle.text = currentArticle.title
        binding.tvHeroCaption.text = currentArticle.title
        binding.tvAuthorName.text = currentArticle.authorName.ifEmpty { "Editorial Staff" }
        binding.tvAuthorRole.text = currentArticle.authorRole
        binding.tvArticleMetaInfo.text = "${currentArticle.source}\n${currentArticle.time}"
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

            val msg = if (current.isSaved) "Article bookmarked" else "Bookmark removed"
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
