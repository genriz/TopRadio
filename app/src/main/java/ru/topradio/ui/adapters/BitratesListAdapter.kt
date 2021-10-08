package ru.topradio.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.topradio.databinding.BitrateItemBinding
import ru.topradio.model.Bitrate

class BitratesListAdapter(private val listener: OnClickListener):
    ListAdapter<Bitrate, BitratesListAdapter.BitrateViewHolder>(Companion) {

    class BitrateViewHolder(val binding: BitrateItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Bitrate>() {
        override fun areItemsTheSame(oldItem: Bitrate, newItem: Bitrate):
                Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Bitrate, newItem: Bitrate):
                Boolean = oldItem.url == newItem.url
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BitrateViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BitrateItemBinding.inflate(layoutInflater,parent,false)
        return BitrateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BitrateViewHolder, position: Int) {
        val bitrate = getItem(position)
        holder.binding.bitrate = bitrate
        holder.binding.bitrateBack.isSelected = bitrate.isSelected
        holder.binding.bitrateText.isSelected = bitrate.isSelected
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            listener.onBitrateClick(position)
        }
    }

    interface OnClickListener{
        fun onBitrateClick(position: Int)
    }
}