package com.example.newsapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newsapp.R
import com.example.newsapp.databinding.ItemNewsCardBinding
import com.example.newsapp.ui.entity.Article

class NewsAdapter : ListAdapter<Article, NewsAdapter.NewsViewHolder>(ArticleDiffCallback()) {

    private var onItemClickListener: ((Article) -> Unit)? = null

    fun setOnItemClickListener(listener: (Article) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(
        private val binding: ItemNewsCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            val context = binding.root.context
            val sourceText = article.source.ifBlank { context.getString(R.string.default_source) }
            val titleText = article.title.ifBlank { context.getString(R.string.default_article_title) }
            val timeText = article.time.ifBlank { context.getString(R.string.default_time_latest) }

            binding.tvArticleSource.text = sourceText.uppercase()
            binding.tvArticleTitle.text = titleText
            binding.tvArticleTime.text = timeText

            if (!article.imageUrl.isNullOrEmpty()) {
                binding.imgArticle.load(article.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
            } else if (article.imageResId != null) {
                binding.imgArticle.setImageResource(article.imageResId)
            } else {
                binding.imgArticle.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(article)
            }
        }
    }

    private class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }
}
