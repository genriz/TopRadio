package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.databinding.PlayerPagerItemBinding
import com.app.topradio.model.Station

class PlayerPagerAdapter: ListAdapter<Station, StationViewHolder>(StationItemDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlayerPagerItemBinding.inflate(layoutInflater,parent,false)
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int){
        val station = getItem(position)
        holder.binding.station = station
        holder.binding.executePendingBindings()
    }
}

class StationItemDiffCallback : DiffUtil.ItemCallback<Station>() {
    override fun areItemsTheSame(oldItem: Station, newItem: Station):
            Boolean = oldItem === newItem
    override fun areContentsTheSame(oldItem: Station, newItem: Station):
            Boolean = oldItem.id == newItem.id
}

class StationViewHolder(val binding: PlayerPagerItemBinding) : RecyclerView.ViewHolder(binding.root)