package com.app.topradio.util

import android.app.Activity
import android.content.Context
import com.app.topradio.model.City
import com.app.topradio.model.Genre
import com.app.topradio.model.Station
import okhttp3.Credentials

object AppData {

    val auth = Credentials.basic("goword", "0GdouNEOMd")

    fun getFavorites(context: Context){
        favorites.addAll(context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("favorites", HashSet<String>())!!)
    }

    val stations = ArrayList<Station>()
    val cities = ArrayList<City>()
    val genres = ArrayList<Genre>()
    var favorites = HashSet<String>()
}