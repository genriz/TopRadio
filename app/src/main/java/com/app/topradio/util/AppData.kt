package com.app.topradio.util

import android.app.Activity
import android.content.Context
import com.app.topradio.model.City
import com.app.topradio.model.Genre
import com.app.topradio.model.Station
import okhttp3.Credentials

object AppData {

    val auth = Credentials.basic("goword", "0GdouNEOMd")
    val stations = ArrayList<Station>()
    val cities = ArrayList<City>()
    val genres = ArrayList<Genre>()
    var favorites = HashSet<String>()
    var viewed = HashSet<String>()

    fun getFavorites(context: Context){
        favorites.clear()
        favorites.addAll(context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("favorites", HashSet<String>())!!)
        stations.forEach {
            if (favorites.contains(it.id.toString())) it.isFavorite = true
        }
    }

    fun getViewed(context: Context){
        viewed.clear()
        viewed.addAll(context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("viewed", HashSet<String>())!!)
        stations.forEach {
            if (viewed.contains(it.id.toString())) it.isViewed = true
        }
    }

    fun getStationById(id: Int): Station {
        var position = 0
        stations.forEach {
            if (it.id == id) position = stations.indexOf(it)
        }
        return stations[position]
    }

    fun getPositionById(id: Int): Int {
        var position = 0
        stations.forEach {
            if (it.id == id) position = stations.indexOf(it)
        }
        return position
    }
}