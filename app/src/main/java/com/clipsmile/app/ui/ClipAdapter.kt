package com.clipsmile.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clipsmile.app.R
import com.clipsmile.app.data.ClipEntry
import com.clipsmile.app.databinding.ItemClipBinding
import com.clipsmile.app.util.ClipFormatter

class ClipAdapter(
    private val onCopyClick: (ClipEntry) -> Unit,
    private val onShareClick: (ClipEntry) -> Unit,
    private val onDeleteClick: (ClipEntry) -> Unit
) : ListAdapter<ClipEntry, ClipAdapter.ClipViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val binding = ItemClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun entryAt(position: Int): ClipEntry = getItem(position)

    inner class ClipViewHolder(private val binding: ItemClipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ClipEntry) {
            binding.clipTextView.text = entry.content
            binding.clipDateView.text = ClipFormatter.formatDate(entry.timestamp)

            binding.root.setOnClickListener { onCopyClick(entry) }
            binding.itemWhatsAppButton.setOnClickListener { onShareClick(entry) }
            binding.itemMoreButton.setOnClickListener { view ->
                showItemMenu(view, entry)
            }
        }

        private fun showItemMenu(anchor: View, entry: ClipEntry) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, R.string.action_copy)
            popup.menu.add(0, 2, 1, R.string.action_share_item)
            popup.menu.add(0, 3, 2, R.string.action_delete_item)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onCopyClick(entry)
                    2 -> onShareClick(entry)
                    3 -> onDeleteClick(entry)
                }
                true
            }
            popup.show()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClipEntry>() {
            override fun areItemsTheSame(oldItem: ClipEntry, newItem: ClipEntry) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ClipEntry, newItem: ClipEntry) =
                oldItem == newItem
        }
    }
}
