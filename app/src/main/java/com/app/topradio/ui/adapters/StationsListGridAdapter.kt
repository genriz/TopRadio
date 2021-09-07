package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.databinding.StationItemListGridBinding
import com.app.topradio.model.Station

class StationsListGridAdapter(private val listener: OnClickListener):
    ListAdapter<Station, StationsListGridAdapter.StationViewHolder>(Companion) {

    class StationViewHolder(val binding: StationItemListGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = StationItemListGridBinding.inflate(layoutInflater,parent,false)
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = getItem(position)
        holder.binding.station = station
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            listener.onStationClick(station)
        }

    }

    interface OnClickListener{
        fun onStationClick(station: Station)
        fun onFavoriteClick(station: Station, position: Int)
    }
}