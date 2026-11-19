package com.example.newsapp.ui.downloads

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.databinding.ActivityDownloadsBinding
import com.example.newsapp.databinding.ItemDownloadHeaderActiveBinding
import com.example.newsapp.databinding.ItemDownloadHeaderCompletedBinding
import com.example.newsapp.ui.base.BaseActivity
import com.example.newsapp.ui.detail.ArticleDetailActivity
import com.example.newsapp.ui.main.MainActivity
import kotlinx.coroutines.launch

class DownloadsActivity :
    BaseActivity<ActivityDownloadsBinding>(ActivityDownloadsBinding::inflate) {

    private val viewModel: DownloadsViewModel by viewModels()

    private val activeHeaderBinding by lazy {
        ItemDownloadHeaderActiveBinding.inflate(layoutInflater)
    }

    private val completedHeaderBinding by lazy {
        ItemDownloadHeaderCompletedBinding.inflate(layoutInflater)
    }

    private val activeHeaderAdapter by lazy { SingleViewAdapter(activeHeaderBinding.root) }
    private val completedHeaderAdapter by lazy { SingleViewAdapter(completedHeaderBinding.root) }

    private val activeAdapter by lazy {
        DownloadsAdapter().apply {
            setOnPauseClickListener { item -> viewModel.toggleItemPause(item.id) }
            setOnCancelClickListener { item -> viewModel.cancelDownload(item.id) }
        }
    }

    private val completedAdapter by lazy {
        DownloadsAdapter().apply {
            setOnItemClickListener { item -> viewModel.onCompletedItemClicked(item.id) }
            setOnMoreClickListener { item, _ -> viewModel.deleteCompleted(item.id) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupBottomNav()
        observeUiState()
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter(
            activeHeaderAdapter,
            activeAdapter,
            completedHeaderAdapter,
            completedAdapter
        )
        binding.rvDownloads.adapter = concatAdapter
    }

    private fun setupListeners() {
        binding.includedHeader.btnClearHistory.setOnClickListener { viewModel.clearHistory() }
        activeHeaderBinding.btnPauseAll.setOnClickListener { viewModel.togglePauseAll() }
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
        val hasActive = activeCount > 0
        activeHeaderBinding.tvActiveCount.text = activeCount.toString()
        activeHeaderBinding.badgeActiveCount.isVisible = hasActive
        activeHeaderBinding.btnPauseAll.isVisible = hasActive
        activeHeaderBinding.btnPauseAll.text =
            if (state.areAllPaused) {
                getString(R.string.downloads_resume_all)
            } else {
                getString(R.string.downloads_pause_all)
            }
        activeHeaderAdapter.isVisible = hasActive

        // Completed list
        completedAdapter.submitList(state.completedDownloads)
        val completedCount = state.completedDownloads.size
        val hasCompleted = completedCount > 0
        completedHeaderBinding.tvCompletedCount.text = completedCount.toString()
        completedHeaderBinding.badgeCompletedCount.isVisible = hasCompleted
        completedHeaderAdapter.isVisible = hasCompleted

        // Empty state view
        binding.tvEmptyDownloads.isVisible = !hasActive && !hasCompleted

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
            tvStorageUsage.text = getString(R.string.downloads_storage_used_format, usedGb, totalGb.toInt())
            tvStorageAvailable.text = getString(R.string.downloads_storage_available_format, availableGb)
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

            tabDownloads.setOnClickListener { binding.rvDownloads.smoothScrollToPosition(0) }

            tabSettings.setOnClickListener {
                Toast.makeText(this@DownloadsActivity, getString(R.string.downloads_settings_tab_selected), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private class SingleViewAdapter(private val view: View) :
        RecyclerView.Adapter<SingleViewAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        var isVisible: Boolean = true
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun getItemCount(): Int = if (isVisible) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(view)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    }
}
