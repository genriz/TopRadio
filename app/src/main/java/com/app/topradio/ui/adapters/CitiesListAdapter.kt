package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R
import com.app.topradio.databinding.CitiesItemListBinding
import com.app.topradio.model.City

class CitiesListAdapter(private val listener: OnClickListener):
    ListAdapter<City, CitiesListAdapter.CityViewHolder>(Companion) {

    class CityViewHolder(val binding: CitiesItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: City, newItem: City):
                Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CitiesItemListBinding.inflate(layoutInflater,parent,false)
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = getItem(position)
        holder.binding.city = city
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            listener.onCityClick(city)
        }
    }

    interface OnClickListener{
        fun onCityClick(city: City)
    }
}