package com.app.topradio.main

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.app.topradio.R
import com.app.topradio.model.City
import com.app.topradio.model.Genre
import com.app.topradio.util.AppData
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
    if (favorite) Glide.with(view).load(R.drawable.ic_favorite_on)
        .placeholder(R.drawable.ic_favorite_off).into(view)
    else Glide.with(view).load(R.drawable.ic_favorite_off_player)
        .placeholder(R.drawable.ic_favorite_off).into(view)
}

@BindingAdapter("setPlayingIcon")
fun setPlayingIcon (view: ImageView, isPlaying: Boolean){
    if (isPlaying) Glide.with(view).load(R.drawable.ic_pause)
        .placeholder(R.drawable.ic_play).into(view)
    else Glide.with(view).load(R.drawable.ic_play)
        .placeholder(R.drawable.ic_play).into(view)
}

@BindingAdapter("setPlayingIconExtended")
fun setPlayingIconExtended (view: ImageView, isPlaying: Boolean){
    if (isPlaying) Glide.with(view).load(R.drawable.ic_pause_extended)
        .placeholder(R.drawable.ic_play_extended).into(view)
    else Glide.with(view).load(R.drawable.ic_play_extended)
        .placeholder(R.drawable.ic_play_extended).into(view)
}

@BindingAdapter("setRecordingIconExtended")
fun setRecordingIconExtended (view: ImageView, isRecording: Boolean){
    if (isRecording) Glide.with(view).load(R.drawable.ic_record_on)
        .placeholder(R.drawable.ic_record_off).into(view)
    else Glide.with(view).load(R.drawable.ic_record_off)
        .placeholder(R.drawable.ic_record_off).into(view)
}

@BindingAdapter("setCityColor")
fun setCityColor (view: TextView, city: City){
    view.text = city.name.first().uppercase()
    val position = AppData.cities.indexOf(city)
    val startColor = ContextCompat.getColor(view.context, R.color.startGradient)
    val endColor = ContextCompat.getColor(view.context, R.color.endGradient)
    val color = ColorUtils.blendARGB(startColor, endColor,
        position.toFloat()/AppData.cities.size.toFloat())
    view.setBackgroundColor(color)
}

@BindingAdapter("setGenreColor")
fun setGenreColor (view: TextView, genre: Genre){
    view.text = genre.name.first().uppercase()
    val position = AppData.genres.indexOf(genre)
    val startColor = ContextCompat.getColor(view.context, R.color.startGradient)
    val endColor = ContextCompat.getColor(view.context, R.color.endGradient)
    val color = ColorUtils.blendARGB(startColor, endColor,
        position.toFloat()/AppData.genres.size.toFloat())
    view.setBackgroundColor(color)
}