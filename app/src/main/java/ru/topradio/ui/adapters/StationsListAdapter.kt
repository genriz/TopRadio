package ru.topradio.ui.adapters

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.*
import ru.topradio.databinding.AdsListItemBinding
import ru.topradio.databinding.StationItemListBinding
import ru.topradio.model.Station

class StationsListAdapter(private val context: Context,
                          private val listener: OnClickListener):
    ListAdapter<Station, RecyclerView.ViewHolder>(Companion) {

    private lateinit var nativeAdLoader: NativeAdLoader
    private var nativeAdViewBinder:NativeAdViewBinder? = null
    private var ads: NativeAd? = null

    class StationViewHolder(val binding: StationItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    class AdsViewHolder(val binding: AdsListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object: DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Station, newItem: Station):
                Boolean = oldItem == newItem
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
            AdsViewHolder(AdsListItemBinding
                .inflate(layoutInflater,parent,false))
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdsViewHolder){
            holder.itemView.visibility = View.GONE
            val params = holder.itemView.layoutParams
            params.height = 0
            params.width = 0
            holder.itemView.layoutParams = params

//            ads?.let{
//                setNativeAdBinder(holder)
//                it.bindNativeAd(nativeAdViewBinder!!)
//                holder.itemView.visibility = View.VISIBLE
//                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
//                params.width = ViewGroup.LayoutParams.MATCH_PARENT
//                holder.itemView.layoutParams = params
//            }

            nativeAdLoader = NativeAdLoader(context)
            nativeAdLoader.setNativeAdLoadListener(object:NativeAdLoadListener{
                override fun onAdLoaded(ad: NativeAd) {
                    try {
                        setNativeAdBinder(holder)
                        ad.bindNativeAd(nativeAdViewBinder!!)
                        holder.itemView.visibility = View.VISIBLE
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        holder.itemView.layoutParams = params
                        ads = ad
                    } catch (e:NativeAdException){
                        try {
                            Handler(Looper.getMainLooper()).postDelayed({
                                nativeAdLoader.loadAd(
                                    NativeAdRequestConfiguration
                                        .Builder("R-M-1619868-3").build()
                                )
                            }, 10000)
                        } catch (e:Exception){}
                    }
                }

                override fun onAdFailedToLoad(p0: AdRequestError) {
                    ads?.let{
                        setNativeAdBinder(holder)
                        it.bindNativeAd(nativeAdViewBinder!!)
                        holder.itemView.visibility = View.VISIBLE
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        holder.itemView.layoutParams = params
                    }
                    if (ads==null){
                        try {
                            Handler(Looper.getMainLooper()).postDelayed({
                                nativeAdLoader.loadAd(
                                    NativeAdRequestConfiguration
                                        .Builder("R-M-1619868-3").build()
                                )
                            }, 10000)
                        } catch (e:Exception){}
                    }
                }

            })

            nativeAdLoader.loadAd(NativeAdRequestConfiguration
                .Builder("R-M-1619868-3").build())

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

    private fun setNativeAdBinder(holder: AdsViewHolder){
        nativeAdViewBinder = NativeAdViewBinder.Builder(holder.binding.adView)
            .setDomainView(TextView(context))
            .setBodyView(holder.binding.adsBody)
            .setTitleView(holder.binding.adsTitle)
            .setIconView(ImageView(context))
            .setMediaView(holder.binding.adsMedia)
            .setFeedbackView(ImageView(context))
            .setSponsoredView(holder.binding.adsSponsored)
            .setCallToActionView(TextView(context))
            .setReviewCountView(TextView(context))
            .setWarningView(TextView(context))
            .setPriceView(TextView(context))
            .setFaviconView(ImageView(context))
            .setAgeView(TextView(context))
            .build()
    }

    interface OnClickListener{
        fun onStationClick(station: Station)
        fun onFavoriteClick(station: Station, position: Int)
    }
}