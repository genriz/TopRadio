package com.app.topradio.util

import android.app.Activity
import android.content.Context
import com.app.topradio.R
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
    val bufferSizes = ArrayList<Int>().apply {
        add(500)
        add(5000)
        add(15000)
    }
    val timerValues = ArrayList<Int>().apply {
        add(0)
        add(10)
        add(20)
        add(30)
        add(40)
        add(50)
        add(60)
        add(70)
        add(80)
        add(90)
        add(100)
    }

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

    fun getSettingBoolean(context: Context, setting: String):Boolean{
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getBoolean(setting, false)
    }
    fun setSettingBoolean(context: Context, setting: String, enabled: Boolean) {
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putBoolean(setting, enabled).apply()
    }
    fun getSettingInt(context: Context, setting: String):Int{
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getInt(setting, 0)
    }
    fun setSettingInt(context: Context, setting: String, value: Int) {
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putInt(setting, value).apply()
    }
    fun getSettingString(context: Context, setting: String):String{
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getString(setting,context.getString(R.string.list))!!
    }
    fun setSettingString(context: Context, setting: String, value: String) {
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putString(setting, value).apply()
    }
    fun getSettingLong(context: Context, setting: String):Long{
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getLong(setting,0L)
    }
    fun setSettingLong(context: Context, setting: String, value: Long) {
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putLong(setting, value).apply()
    }
}