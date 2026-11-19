package com.example.newsapp.ui.downloads

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.databinding.ItemDownloadActiveBinding
import com.example.newsapp.databinding.ItemDownloadCompletedBinding

enum class DownloadFormat(
    @get:ColorRes val bgResId: Int,
    @get:ColorRes val textResId: Int
) {
    PDF(R.color.download_badge_pdf_bg, R.color.download_badge_pdf_text),
    HTML(R.color.download_badge_html_bg, R.color.download_badge_html_text),
    IMAGE(R.color.download_badge_jpg_bg, R.color.download_badge_jpg_text),
    OTHER(R.color.download_badge_completed_bg, R.color.download_badge_completed_text);

    companion object {
        fun from(format: String): DownloadFormat = when (format.uppercase()) {
            "PDF" -> PDF
            "HTML" -> HTML
            "JPG", "PNG", "JPEG" -> IMAGE
            else -> OTHER
        }
    }
}

interface HasId {
    val id: String
}

class GenericDiffCallback<T : HasId> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}

sealed interface DownloadItem : HasId {
    data class Active(
        override val id: String,
        val title: String,
        val currentSizeMb: Double,
        val totalSizeMb: Double,
        val format: String,
        val speed: String,
        val progress: Int,
        val isPaused: Boolean = false,
        val imageRes: Int = R.mipmap.ic_launcher
    ) : DownloadItem

    data class Completed(
        override val id: String,
        val title: String,
        val format: String,
        val sizeMb: Double,
        val completedTime: String,
        val imageRes: Int = R.mipmap.ic_launcher
    ) : DownloadItem
}

typealias ActiveDownloadItem = DownloadItem.Active
typealias CompletedDownloadItem = DownloadItem.Completed

class DownloadsAdapter : ListAdapter<DownloadItem, RecyclerView.ViewHolder>(GenericDiffCallback()) {

    private var onPauseClickListener: ((DownloadItem.Active) -> Unit)? = null
    private var onCancelClickListener: ((DownloadItem.Active) -> Unit)? = null
    private var onItemClickListener: ((DownloadItem.Completed) -> Unit)? = null
    private var onMoreClickListener: ((DownloadItem.Completed, View) -> Unit)? = null

    fun setOnPauseClickListener(listener: (DownloadItem.Active) -> Unit) {
        this.onPauseClickListener = listener
    }

    fun setOnCancelClickListener(listener: (DownloadItem.Active) -> Unit) {
        this.onCancelClickListener = listener
    }

    fun setOnItemClickListener(listener: (DownloadItem.Completed) -> Unit) {
        this.onItemClickListener = listener
    }

    fun setOnMoreClickListener(listener: (DownloadItem.Completed, View) -> Unit) {
        this.onMoreClickListener = listener
    }

    companion object {
        private const val VIEW_TYPE_ACTIVE = 1
        private const val VIEW_TYPE_COMPLETED = 2
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DownloadItem.Active -> VIEW_TYPE_ACTIVE
        is DownloadItem.Completed -> VIEW_TYPE_COMPLETED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ACTIVE -> ActiveViewHolder(
                ItemDownloadActiveBinding.inflate(inflater, parent, false)
            )
            else -> CompletedViewHolder(
                ItemDownloadCompletedBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ActiveViewHolder -> holder.bind(item as DownloadItem.Active, onPauseClickListener, onCancelClickListener)
            is CompletedViewHolder -> holder.bind(
                item = item as DownloadItem.Completed,
                isLastItem = position == itemCount - 1,
                onItemClick = onItemClickListener,
                onMoreClick = onMoreClickListener
            )
        }
    }

    class ActiveViewHolder(
        private val binding: ItemDownloadActiveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: DownloadItem.Active,
            onPauseClick: ((DownloadItem.Active) -> Unit)?,
            onCancelClick: ((DownloadItem.Active) -> Unit)?
        ) {
            val context = itemView.context
            with(binding) {
                tvActiveTitle.text = item.title
                tvActiveSize.text = context.getString(
                    R.string.downloads_size_progress_format,
                    item.currentSizeMb,
                    item.totalSizeMb
                )
                tvActiveFormatBadge.text = item.format
                tvActivePercent.text = context.getString(R.string.downloads_percent_format, item.progress)
                pbActive.progress = item.progress
                imgActiveThumb.setImageResource(item.imageRes)

                if (item.isPaused) {
                    tvActiveSpeed.text = context.getString(R.string.downloads_speed_paused)
                    tvPauseItem.text = context.getString(R.string.downloads_btn_resume)
                    icPauseItem.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    tvActiveSpeed.text = item.speed
                    tvPauseItem.text = context.getString(R.string.downloads_btn_pause)
                    icPauseItem.setImageResource(R.drawable.ic_pause)
                }

                btnPauseItem.setOnClickListener { onPauseClick?.invoke(item) }
                btnCancelItem.setOnClickListener { onCancelClick?.invoke(item) }
            }
        }
    }

    class CompletedViewHolder(
        private val binding: ItemDownloadCompletedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: DownloadItem.Completed,
            isLastItem: Boolean,
            onItemClick: ((DownloadItem.Completed) -> Unit)?,
            onMoreClick: ((DownloadItem.Completed, View) -> Unit)?
        ) {
            val context = itemView.context
            with(binding) {
                tvCompletedTitle.text = item.title
                tvCompletedTypeBadge.text = item.format
                tvCompletedSize.text = context.getString(R.string.downloads_size_mb_format, item.sizeMb)
                tvCompletedTime.text = context.getString(R.string.downloads_time_format, item.completedTime)
                imgCompletedThumb.setImageResource(item.imageRes)

                // Dynamic badge color based on format enum
                val badgeFormat = DownloadFormat.from(item.format)
                tvCompletedTypeBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, badgeFormat.bgResId)
                )
                tvCompletedTypeBadge.setTextColor(
                    ContextCompat.getColor(context, badgeFormat.textResId)
                )

                // Hide bottom divider for last item
                dividerCompleted.isVisible = !isLastItem

                root.setOnClickListener { onItemClick?.invoke(item) }
                btnMoreCompleted.setOnClickListener { view -> onMoreClick?.invoke(item, view) }
            }
        }
    }
}
