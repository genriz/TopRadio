package ru.topradio.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.topradio.databinding.AdsListItemBinding
import ru.topradio.databinding.StationItemListBinding
import ru.topradio.model.Station
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StationsListAdapter(private val context: Context,
                          private val listener: OnClickListener):
    ListAdapter<Station, RecyclerView.ViewHolder>(Companion) {

    private lateinit var adLoader: AdLoader

    class StationViewHolder(val binding: StationItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    class AdsViewHolder(val binding: AdsListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem.id == newItem.id
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isAds) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType==0){
            StationViewHolder(StationItemListBinding
                .inflate(layoutInflater,parent,false))
        } else {
            val holder = AdsViewHolder(AdsListItemBinding
                .inflate(layoutInflater,parent,false))
            adLoader = AdLoader.Builder(context,
                "ca-app-pub-8287740228306736/3700859131")
                .forNativeAd { ad : NativeAd ->
                    CoroutineScope(Dispatchers.Main).launch {
                        holder.binding.adView.setNativeAd(ad)
                        holder.binding.adView.visibility = View.VISIBLE
                    }
                }
                .build()
            holder
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdsViewHolder){
            holder.binding.adView.visibility = View.INVISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                adLoader.loadAd(AdRequest.Builder().build())
            }
        }
        if (holder is StationViewHolder){
            val station = getItem(position)
            holder.binding.station = station
            holder.binding.executePendingBindings()
            holder.binding.favorite.setOnClickListener {
                listener.onFavoriteClick(station, position)
            }
            holder.binding.root.setOnClickListener {
                listener.onStationClick(station)
            }
        }
    }

    interface OnClickListener{
        fun onStationClick(station: Station)
        fun onFavoriteClick(station: Station, position: Int)
    }
}