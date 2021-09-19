package com.app.topradio.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R
import com.app.topradio.databinding.PlayerPagerItemBinding
import com.app.topradio.model.Station
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*


class PlayerPagerAdapter(private val context: Context, private val listener: OnClick)
    : ListAdapter<Station, StationViewHolder>(StationItemDiffCallback()),
    BitratesListAdapter.OnClickListener {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlayerPagerItemBinding.inflate(layoutInflater,parent,false)
        return StationViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: StationViewHolder, position: Int){
        val station = getItem(position)
        holder.binding.station = station
        holder.binding.trackNameExpanded.setOnClickListener {
            listener.onCopyClick(station.track)
        }
        holder.binding.trackNameExpanded.isSelected = true
        Glide.with(holder.binding.imageView3).load(R.raw.player_bars_up)
            .into(holder.binding.imageView3)
        Glide.with(holder.binding.imageView4).load(R.raw.player_bars_down)
            .into(holder.binding.imageView4)
        val bitratesAdapter = BitratesListAdapter(this)
        holder.binding.adapter = bitratesAdapter
        bitratesAdapter.submitList(station.bitrates)
        holder.binding.recyclerBitrates.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.HORIZONTAL, false)
        holder.binding.executePendingBindings()

        holder.binding.adsBannerContainer.visibility = View.GONE
        holder.binding.iconCardExpanded.visibility = View.VISIBLE

        holder.binding.adsView.adListener = object: AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                holder.binding.adsView.loadAd(AdRequest.Builder().build())
            }
            override fun onAdLoaded() {
                super.onAdLoaded()
                handler.removeCallbacksAndMessages(null)
                holder.binding.iconCardExpanded.visibility = View.INVISIBLE
                holder.binding.adsBannerContainer.visibility = View.VISIBLE
                handler.postDelayed({
                    holder.binding.adsView.loadAd(AdRequest.Builder().build())
                },120000)
            }
            override fun onAdClosed() {
                super.onAdClosed()
                holder.binding.iconCardExpanded.visibility = View.VISIBLE
                holder.binding.adsBannerContainer.visibility = View.GONE
            }
            override fun onAdClicked() {
                super.onAdClicked()
                holder.binding.iconCardExpanded.visibility = View.VISIBLE
                holder.binding.adsBannerContainer.visibility = View.GONE
            }
            override fun onAdOpened() {
                super.onAdOpened()
                holder.binding.iconCardExpanded.visibility = View.VISIBLE
                holder.binding.adsBannerContainer.visibility = View.GONE
            }
        }

        holder.binding.adsView.loadAd(AdRequest.Builder().build())

        holder.binding.adsClose.setOnClickListener {
            holder.binding.iconCardExpanded.visibility = View.VISIBLE
            holder.binding.adsBannerContainer.visibility = View.GONE
        }

    }

    override fun onViewDetachedFromWindow(holder: StationViewHolder) {
        handler.removeCallbacksAndMessages(null)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onBitrateClick(position: Int) {
        listener.onBitrateClick(position)
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
    fun onBitrateClick(position: Int)
}