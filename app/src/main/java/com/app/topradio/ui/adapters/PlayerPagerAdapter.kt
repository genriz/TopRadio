package com.app.topradio.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R
import com.app.topradio.databinding.PlayerPagerItemBinding
import com.app.topradio.model.Station
import com.bumptech.glide.Glide


class PlayerPagerAdapter(private val listener: OnClick): ListAdapter<Station, StationViewHolder>(StationItemDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlayerPagerItemBinding.inflate(layoutInflater,parent,false)
        return StationViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: StationViewHolder, position: Int){
        val station = getItem(position)
        holder.binding.station = station
        holder.binding.trackNameExpanded.setOnTouchListener { _, _ ->
            listener.onCopyClick(station.track)
            true
        }
        holder.binding.trackNameExpanded.isSelected = true
        Glide.with(holder.binding.imageView3).load(R.raw.player_bars_up)
            .into(holder.binding.imageView3)
        Glide.with(holder.binding.imageView4).load(R.raw.player_bars_down)
            .into(holder.binding.imageView4)
        holder.binding.executePendingBindings()
    }
}

class StationItemDiffCallback : DiffUtil.ItemCallback<Station>() {
    override fun areItemsTheSame(oldItem: Station, newItem: Station):
            Boolean = oldItem === newItem
    override fun areContentsTheSame(oldItem: Station, newItem: Station):
            Boolean = oldItem.id == newItem.id
}

class StationViewHolder(val binding: PlayerPagerItemBinding) :
    RecyclerView.ViewHolder(binding.root)

interface OnClick{
    fun onCopyClick(text: String)
}