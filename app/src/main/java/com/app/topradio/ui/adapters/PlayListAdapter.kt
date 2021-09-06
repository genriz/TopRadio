package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.databinding.PlaylistIemBinding
import com.app.topradio.model.PlaylistItem

class PlayListAdapter():
    ListAdapter<PlaylistItem, PlayListAdapter.CityViewHolder>(Companion) {

    class CityViewHolder(val binding: PlaylistIemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<PlaylistItem>() {
        override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem):
                Boolean = oldItem.song == newItem.song
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlaylistIemBinding.inflate(layoutInflater,parent,false)
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val playlistItem = getItem(position)
        holder.binding.playlistItem = playlistItem
        holder.binding.executePendingBindings()
    }
}