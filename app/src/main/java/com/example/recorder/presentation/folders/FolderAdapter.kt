package com.example.recorder.presentation.folders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recorder.R
import com.example.recorder.domain.model.Folder

class FolderAdapter(
    private val onFolderClick: (Folder) -> Unit,
    private val onMenuClick: (View, Folder) -> Unit
) : ListAdapter<FolderAdapter.FolderItem, FolderAdapter.ViewHolder>(DiffCallback()) {

    data class FolderItem(val folder: Folder, val recordingCount: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvFolderName)
        private val tvCount: TextView = view.findViewById(R.id.tvFolderCount)
        private val btnMenu: View = view.findViewById(R.id.btnFolderMenu)

        fun bind(item: FolderItem) {
            tvName.text = item.folder.name
            tvCount.text = itemView.context.resources.getQuantityString(
                R.plurals.folder_recording_count, item.recordingCount, item.recordingCount
            )
            itemView.setOnClickListener { onFolderClick(item.folder) }
            btnMenu.setOnClickListener { onMenuClick(it, item.folder) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FolderItem>() {
        override fun areItemsTheSame(a: FolderItem, b: FolderItem) = a.folder.id == b.folder.id
        override fun areContentsTheSame(a: FolderItem, b: FolderItem) = a == b
    }
}
