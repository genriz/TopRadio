package com.app.topradio.main

import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R
import com.bumptech.glide.Glide


@BindingAdapter(value = ["setAdapter"])
fun RecyclerView.bindRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>) {
    this.run {
        this.adapter = adapter
        val itemDecorator = DividerItemDecoration(
            context, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(
            ContextCompat.getDrawable(
                this.context,
                R.drawable.divider)!!)
        this.addItemDecoration(itemDecorator)
    }
}

@BindingAdapter("setIcon")
fun setIcon (view: ImageView, path: String){
    Glide.with(view).load("https://top-radio.ru/assets/image/radio/180/$path").into(view)
}

@BindingAdapter("setFavorite")
fun setFavorite (view: ImageView, favorite: Boolean){
    if (favorite) Glide.with(view).load(R.drawable.ic_favorite_on)
        .placeholder(R.drawable.ic_favorite_off).into(view)
    else Glide.with(view).load(R.drawable.ic_favorite_off)
        .placeholder(R.drawable.ic_favorite_off).into(view)
}

@BindingAdapter("setFavoritePlayer")
fun setFavoritePlayer (view: ImageView, favorite: Boolean){
    if (favorite) Glide.with(view).load(R.drawable.ic_favorite_on).into(view)
    else Glide.with(view).load(R.drawable.ic_favorite_off_player).into(view)
}

@BindingAdapter("setPlayingIcon")
fun setPlayingIcon (view: ImageView, isPlaying: Boolean){
    if (isPlaying) Glide.with(view).load(R.drawable.ic_pause)
        .placeholder(R.drawable.ic_play).into(view)
    else Glide.with(view).load(R.drawable.ic_play)
        .placeholder(R.drawable.ic_play).into(view)
}

@BindingAdapter("setCityColor")
fun setCityColor (view: TextView, name: String){

}