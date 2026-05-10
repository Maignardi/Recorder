package com.example.recorder.presentation.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recorder.R
import com.example.recorder.databinding.ItemRecordingBinding
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.util.toFormattedDuration
import com.example.recorder.util.toFormattedSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    var folder: RecordingFolder,
    private val onItemClick: (Recording) -> Unit,
    private val onPlayClick: (Recording) -> Unit,
    private val onFavoriteClick: (Recording) -> Unit,
    private val onMenuClick: (View, Recording) -> Unit,
    private val onLongClick: (Recording) -> Unit,
    private val onSelectionToggle: (Recording) -> Unit
) : ListAdapter<Recording, RecordingAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var selectedIds = emptySet<Long>()
    private var isSelectionMode = false

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds = ids
        isSelectionMode = ids.isNotEmpty()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: Recording) {
            val isSelected = selectedIds.contains(recording.id)
            binding.apply {
                root.cardElevation = if (isSelected) 4f else 0f
                root.strokeWidth = if (isSelected) 2 else 0
                root.strokeColor = root.context.getColor(R.color.primary)
                
                ivSelection.visibility = if (isSelected) View.VISIBLE else View.GONE
                ivPlayIcon.visibility = if (isSelected) View.GONE else View.VISIBLE

                tvName.text = recording.name
                tvDuration.text = recording.durationMs.toFormattedDuration()
                tvSize.text = recording.sizeBytes.toFormattedSize()
                tvDate.text = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                    .format(Date(recording.createdAt))

                if (folder == RecordingFolder.TRASH) {
                    btnFavorite.visibility = View.GONE
                } else {
                    btnFavorite.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                    btnFavorite.setImageResource(
                        if (recording.isFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                    btnFavorite.setOnClickListener { onFavoriteClick(recording) }
                }

                btnMore.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                btnMore.setOnClickListener { onMenuClick(it, recording) }
                ivPlayIcon.setOnClickListener { onPlayClick(recording) }
                
                root.setOnClickListener {
                    if (isSelectionMode) onSelectionToggle(recording)
                    else onItemClick(recording)
                }
                
                root.setOnLongClickListener {
                    onLongClick(recording)
                    true
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Recording>() {
            override fun areItemsTheSame(a: Recording, b: Recording) = a.id == b.id
            override fun areContentsTheSame(a: Recording, b: Recording) = a == b
        }
    }
}
