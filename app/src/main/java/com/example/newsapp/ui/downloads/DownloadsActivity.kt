package com.example.newsapp.ui.downloads

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.newsapp.R
import com.example.newsapp.databinding.ActivityDownloadsBinding
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.detail.ArticleDetailActivity
import com.example.newsapp.ui.main.MainActivity
import java.util.Locale
import kotlinx.coroutines.launch

class DownloadsActivity :
    BaseActivity<ActivityDownloadsBinding>(ActivityDownloadsBinding::inflate) {

    private val viewModel: DownloadsViewModel by viewModels()

    private lateinit var activeAdapter: DownloadsAdapter
    private lateinit var completedAdapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        setupBottomNav()
        observeUiState()
    }

    private fun setupRecyclerViews() {
        activeAdapter =
            DownloadsAdapter(
                onPauseClick = { item -> viewModel.toggleItemPause(item.id) },
                onCancelClick = { item -> viewModel.cancelDownload(item.id) },
            )
        binding.rvActiveDownloads.adapter = activeAdapter

        completedAdapter =
            DownloadsAdapter(
                onItemClick = { item -> viewModel.onCompletedItemClicked(item.id) },
                onMoreClick = { item, _ -> viewModel.deleteCompleted(item.id) },
            )
        binding.rvCompletedDownloads.adapter = completedAdapter
    }

    private fun setupListeners() {
        binding.includedHeader.btnClearHistory.setOnClickListener { viewModel.clearHistory() }

        binding.btnPauseAll.setOnClickListener { viewModel.togglePauseAll() }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderUiState(state) }
            }
        }
    }

    private fun renderUiState(state: DownloadsUiState) {
        // Active list & controls
        activeAdapter.submitList(state.activeDownloads)
        val activeCount = state.activeDownloads.size
        binding.tvActiveCount.text = activeCount.toString()
        val hasActive = activeCount > 0
        binding.badgeActiveCount.visibility = if (hasActive) View.VISIBLE else View.GONE
        binding.btnPauseAll.visibility = if (hasActive) View.VISIBLE else View.GONE
        binding.rvActiveDownloads.visibility = if (hasActive) View.VISIBLE else View.GONE

        binding.btnPauseAll.text =
            if (state.areAllPaused) {
                getString(R.string.downloads_resume_all)
            } else {
                getString(R.string.downloads_pause_all)
            }

        // Completed list
        completedAdapter.submitList(state.completedDownloads)
        val completedCount = state.completedDownloads.size
        binding.tvCompletedCount.text = completedCount.toString()
        val hasCompleted = completedCount > 0
        binding.badgeCompletedCount.visibility = if (hasCompleted) View.VISIBLE else View.GONE
        binding.cardCompletedList.visibility = if (hasCompleted) View.VISIBLE else View.GONE

        // Empty state view
        binding.tvEmptyDownloads.visibility =
            if (!hasActive && !hasCompleted) View.VISIBLE else View.GONE

        // Open article detail navigation
        state.openArticleEvent?.let { article ->
            val intent =
                Intent(this, ArticleDetailActivity::class.java).apply {
                    putExtra(ArticleDetailActivity.EXTRA_ARTICLE, article)
                }
            startActivity(intent)
            viewModel.onOpenArticleEventHandled()
        }

        // Storage Info Component
        val usedGb = state.usedStorageGb
        val totalGb = state.totalStorageGb
        val availableGb = (totalGb - usedGb).coerceAtLeast(0.0)
        val progressPercent = ((usedGb / totalGb) * 100).toInt()

        with(binding.includedHeader) {
            tvStorageUsage.text =
                String.format(Locale.US, "%.1f GB / %d GB used", usedGb, totalGb.toInt())
            tvStorageAvailable.text = String.format(Locale.US, "%.1f GB available", availableGb)
            pbStorage.progress = progressPercent
        }

        // Toast feedback
        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    private fun setupBottomNav() {
        with(binding.includedBottomNav) {
            tabHome.setOnClickListener {
                val intent =
                    Intent(this@DownloadsActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                startActivity(intent)
                finish()
            }

            tabDownloads.setOnClickListener { binding.scrollDownloads.smoothScrollTo(0, 0) }

            tabSettings.setOnClickListener {
                Toast.makeText(this@DownloadsActivity, "Settings tab selected", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}
