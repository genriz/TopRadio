package ru.topradio.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.topradio.R
import ru.topradio.databinding.PlayerPagerItemBinding
import ru.topradio.model.Station
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*


class PlayerPagerAdapter(private val context: Context, private val listener: OnClick,
                         private val showAds: MutableLiveData<Boolean>,
                         private val lifecycleOwner: LifecycleOwner)
    : ListAdapter<Station, StationViewHolder>(StationItemDiffCallback()),
    BitratesListAdapter.OnClickListener {

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
            if (holder.binding.trackNameExpanded.text.isNotEmpty())
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
//                holder.binding.adsView.loadAd(AdRequest.Builder().build())
            }
            override fun onAdLoaded() {
                super.onAdLoaded()
                holder.binding.iconCardExpanded.visibility = View.INVISIBLE
                holder.binding.adsBannerContainer.visibility = View.VISIBLE
//                showAds.postValue(false)
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

        showAds.observe(lifecycleOwner,{
            it?.let{ show ->
                if (show) holder.binding.adsView.loadAd(AdRequest.Builder().build())
                else {
                    holder.binding.iconCardExpanded.visibility = View.VISIBLE
                    holder.binding.adsBannerContainer.visibility = View.GONE
                }
            }
        })

        holder.binding.adsClose.setOnClickListener {
            holder.binding.iconCardExpanded.visibility = View.VISIBLE
            holder.binding.adsBannerContainer.visibility = View.GONE
            showAds.postValue(false)
        }

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