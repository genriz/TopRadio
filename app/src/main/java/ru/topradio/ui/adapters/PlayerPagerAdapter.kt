package ru.topradio.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import ru.topradio.R
import ru.topradio.databinding.PlayerPagerItemBinding
import ru.topradio.model.Station
import com.bumptech.glide.Glide
import com.yandex.mobile.ads.banner.AdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import ru.topradio.model.MainViewModel


class PlayerPagerAdapter(private val context: Context, private val listener: OnClick,
                         private val viewModel: MainViewModel,
                         private val lifecycleOwner: LifecycleOwner)
    : ListAdapter<Station, PlayerPagerAdapter.StationViewHolder>(StationItemDiffCallback()),
    BitratesListAdapter.OnClickListener {

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlayerPagerItemBinding.inflate(layoutInflater,parent,false)

        binding.adsView.setAdUnitId("R-M-1619868-2")
        binding.adsView.setAdSize(AdSize.flexibleSize(250,250))

        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int,
                                  payloads: MutableList<Any>) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else holder.binding.trackNameExpanded.text = payloads[0].toString()
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
        Glide.with(context).load(R.raw.player_bars_up)
            .into(holder.binding.imageView3)
        Glide.with(context).load(R.raw.player_bars_down)
            .into(holder.binding.imageView4)

        val bitratesAdapter = BitratesListAdapter(this)
        holder.binding.recyclerBitrates.layoutManager = StaggeredGridLayoutManager(station.bitrates.size,
            LinearLayoutManager.VERTICAL)
        holder.binding.adapter = bitratesAdapter
        bitratesAdapter.submitList(station.bitrates)

        holder.binding.executePendingBindings()

        holder.binding.adsView.setBannerAdEventListener(object:BannerAdEventListener{
            override fun onAdLoaded() {
                if (viewModel.loadAds.value!!) {
                    holder.binding.iconCardExpanded.visibility = View.INVISIBLE
                    holder.binding.adsBannerContainer.visibility = View.VISIBLE
                }
            }

            override fun onAdFailedToLoad(p0: AdRequestError) {
                try {
                    Handler(Looper.getMainLooper()).postDelayed({
                        holder.binding.adsView.loadAd(AdRequest.Builder().build())
                    }, 10000)
                } catch (e:Exception){}
            }

            override fun onAdClicked() {
                viewModel.loadAds.postValue(false)
                holder.binding.iconCardExpanded.visibility = View.VISIBLE
                holder.binding.adsBannerContainer.visibility = View.GONE
            }

            override fun onLeftApplication() {
                viewModel.loadAds.postValue(false)
                holder.binding.iconCardExpanded.visibility = View.VISIBLE
                holder.binding.adsBannerContainer.visibility = View.GONE
            }

            override fun onReturnedToApplication() {

            }

            override fun onImpression(p0: ImpressionData?) {

            }

        })

        viewModel.loadAds.observe(lifecycleOwner) {
            it?.let { loadAd ->
                if (loadAd) {
                    holder.binding.adsView.loadAd(AdRequest.Builder().build())
                } else {
                    holder.binding.iconCardExpanded.visibility = View.VISIBLE
                    holder.binding.adsBannerContainer.visibility = View.GONE
                }
            }
        }

        holder.binding.adsClose.setOnClickListener {
            holder.binding.iconCardExpanded.visibility = View.VISIBLE
            holder.binding.adsBannerContainer.visibility = View.GONE
            viewModel.loadAds.postValue(false)
        }
    }

    override fun onBitrateClick(position: Int) {
        listener.onBitrateClick(position)
    }

    class StationItemDiffCallback : DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem.track == newItem.track
    }

    class StationViewHolder(val binding: PlayerPagerItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface OnClick{
        fun onCopyClick(text: String)
        fun onBitrateClick(position: Int)
    }

}