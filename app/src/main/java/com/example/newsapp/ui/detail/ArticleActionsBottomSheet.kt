package com.example.newsapp.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.newsapp.R
import com.example.newsapp.databinding.BottomSheetArticleActionsBinding
import com.example.newsapp.ui.entity.Article
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ArticleActionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetArticleActionsBinding? = null
    private val binding get() = _binding!!

    private var article: Article? = null
    private var onBookmarkToggle: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetArticleActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvModalArticleTitle.text = article?.title ?: getString(R.string.default_article_title)

        binding.layoutActionOpenBrowser.setOnClickListener {
            dismiss()
            openUrl(article?.url)
        }

        binding.layoutActionCopyLink.setOnClickListener {
            dismiss()
            copyLinkToClipboard(article?.url)
        }

        binding.layoutActionShare.setOnClickListener {
            dismiss()
            shareArticle(article)
        }

        binding.layoutActionBookmark.setOnClickListener {
            dismiss()
            onBookmarkToggle?.invoke()
        }

        binding.btnModalClose.setOnClickListener {
            dismiss()
        }
    }

    private fun openUrl(url: String?) {
        if (!url.isNullOrEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.detail_cannot_open_url), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.detail_url_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyLinkToClipboard(url: String?) {
        if (!url.isNullOrEmpty()) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Article Link", url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.msg_link_copied), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.detail_url_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareArticle(article: Article?) {
        if (article == null) return
        val shareText = "${article.title}\n${article.url ?: ""}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.detail_share_chooser_title)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ArticleActionsBottomSheet"

        fun newInstance(
            article: Article,
            onBookmarkToggle: () -> Unit
        ): ArticleActionsBottomSheet {
            return ArticleActionsBottomSheet().apply {
                this.article = article
                this.onBookmarkToggle = onBookmarkToggle
            }
        }
    }
}
