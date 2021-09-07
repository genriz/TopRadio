package com.app.topradio.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R

class DayAdapter(private val days: ArrayList<String>): RecyclerView.Adapter<DayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val day: TextView = view.findViewById(R.id.day)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_item,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.day.text = days[position]
        holder.day.isSelected = false
        holder.day.setOnClickListener {
            holder.day.isSelected = !holder.day.isSelected
        }
    }

    override fun getItemCount(): Int {
        return days.count()
    }
}