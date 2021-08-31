package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.databinding.RecordsItemListBinding
import com.app.topradio.model.Record

class RecordsListAdapter(private val listener: OnClickListener):
    ListAdapter<Record, RecordsListAdapter.BitrateViewHolder>(Companion) {

    class BitrateViewHolder(val binding: RecordsItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: Record, newItem: Record):
                Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BitrateViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RecordsItemListBinding.inflate(layoutInflater,parent,false)
        return BitrateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BitrateViewHolder, position: Int) {
        val record = getItem(position)
        holder.binding.record = record
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener {
            listener.onRecordClick(record)
        }
    }

    interface OnClickListener{
        fun onRecordClick(record: Record)
    }
}