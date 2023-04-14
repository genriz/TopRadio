package ru.topradio.util

import android.app.Activity
import android.content.Context
import ru.topradio.R
import ru.topradio.model.City
import ru.topradio.model.Genre
import ru.topradio.model.Station
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Credentials
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object AppData {

    val auth = Credentials.basic("goword", "0GdouNEOMd")
    val stations = ArrayList<Station>()
    var stationsPlayer = ArrayList<Station>()
    val cities = ArrayList<City>()
    val genres = ArrayList<Genre>()
    var favorites = HashSet<String>()
    val calDays = ArrayList<Int>().apply {
        addAll(arrayOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY))
    }

    fun getFavorites(context: Context){
        favorites.clear()
        favorites.addAll(context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("favorites", HashSet<String>())!!)
        stations.forEach {
            if (favorites.contains(it.id.toString())) it.isFavorite = true
        }
    }

    fun getStationById(id: Int): Station {
        return if (stations.size>0) {
            var position = 0
            stations.forEach {
                if (it.id == id) position = stations.indexOf(it)
            }
            stations[position]
        } else Station()
    }

    fun getRepeatDays(context: Context): HashSet<String>{
        val repeatDays = HashSet<String>()
        repeatDays.addAll(context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("repeatDays", HashSet<String>())!!)
        return repeatDays
    }
    fun setRepeatDays(context: Context, days: HashSet<String>){
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putStringSet("repeatDays", days).apply()
    }

    fun getSettingBoolean(context: Context, setting: String):Boolean{
        return if (setting=="headphone"||setting=="autoplay"){
            context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
                .getBoolean(setting, true)
        } else {
            context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
                .getBoolean(setting, false)
        }
    }
    fun setSettingBoolean(context: Context, setting: String, enabled: Boolean) {
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putBoolean(setting, enabled).apply()
    }
    fun getSettingInt(context: Context, setting: String):Int{
        return context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getInt(setting, -1)
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

    fun saveStationViewedList(context: Context, list: ArrayList<Station>) {
        val gson = Gson()
        val json: String = gson.toJson(list)
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE).edit()
            .putString("viewedStations", json).apply()
    }

    fun getStationViewedList(context: Context): ArrayList<Station> {
        val gson = Gson()
        val json: String = context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getString("viewedStations", "")!!
        val type: Type = object : TypeToken<ArrayList<Station>>() {}.type
        return if (json=="") ArrayList()
            else gson.fromJson(json, type)
    }

    fun arraysEqualsContent(array1: ArrayList<Station>,
                            array2: ArrayList<Station>):Boolean{
        var equals = true
        if (array1.size==array2.size){
            for (i in 0 until array1.size){
                if (array1[i].id!=array2[i].id) {
                    equals = false
                    break
                }
            }
        } else equals = false
        return equals
    }
}