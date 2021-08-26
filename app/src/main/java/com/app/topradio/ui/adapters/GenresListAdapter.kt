package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.databinding.GenresItemListBinding
import com.app.topradio.model.Genre

class GenresListAdapter(private val listener: OnClickListener):
    ListAdapter<Genre, GenresListAdapter.GenreViewHolder>(Companion) {

    class GenreViewHolder(val binding: GenresItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Genre>() {
        override fun areItemsTheSame(oldItem: Genre, newItem: Genre):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: Genre, newItem: Genre):
                Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = GenresItemListBinding.inflate(layoutInflater,parent,false)
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = getItem(position)
        holder.binding.genre = genre
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            listener.onGenreClick(genre)
        }
    }

    interface OnClickListener{
        fun onGenreClick(genre: Genre)
    }
}