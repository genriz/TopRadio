package com.app.topradio.model

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.topradio.util.AppData
import com.app.topradio.api.ApiRadio
import com.app.topradio.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class MainViewModel: ViewModel() {
    val stationsApi = MutableLiveData<ArrayList<Station>>()
    val stations: LiveData<ArrayList<Station>> = stationsApi

    val station = MutableLiveData<Station>().apply { value = Station() }
    val stationPager = MutableLiveData<Station>().apply { value = Station() }

    private val genresApi = MutableLiveData<ArrayList<Genre>>().apply {
        AppData.genres.forEach { genre ->
            var count = 0
            AppData.stations.forEach { station ->
                if (station.genres.contains(genre.id)) count++
            }
            genre.count = count
        }
        value = AppData.genres
    }
    val genres: LiveData<ArrayList<Genre>> = genresApi

    private val citiesApi = MutableLiveData<ArrayList<City>>().apply {
        AppData.cities.forEach { city ->
            var count = 0
            AppData.stations.forEach { station ->
                if (station.cities.contains(city.id)) count++
            }
            city.count = count
        }
        value = AppData.cities
    }
    val cities: LiveData<ArrayList<City>> = citiesApi

    val updateItemPosition = MutableLiveData<Int>()
    val playerWaiting = MutableLiveData<Boolean>()
    val playerRecording = MutableLiveData<Boolean>().apply { value = false }
    val recordTime = MutableLiveData<String>()

    fun getAllStations(){
        stationsApi.postValue(AppData.stations)
    }

    fun getFavoriteStations(){
        val favorites = ArrayList<Station>()
        AppData.stations.forEach {station->
            if (station.isFavorite) favorites.add(station)
        }
        stationsApi.postValue(favorites)
    }

    fun getViewedStations(){
        val viewed = ArrayList<Station>()
        AppData.stations.forEach {station->
            if (station.isViewed) viewed.add(station)
        }
        stationsApi.postValue(viewed)
    }

    fun getGenreStations(genreId: Int){
        val genreStations = ArrayList<Station>()
        AppData.stations.forEach {station->
            if (station.genres.contains(genreId)) genreStations.add(station)
        }
        stationsApi.postValue(genreStations)
    }

    fun getCityStations(cityId: Int){
        val cityStations = ArrayList<Station>()
        AppData.stations.forEach {station->
            if (station.cities.contains(cityId)) cityStations.add(station)
        }
        stationsApi.postValue(cityStations)
    }

    fun searchStations (query: String){
        val stations = ArrayList<Station>()
        AppData.stations.forEach {
            if (it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        stationsApi.postValue(stations)
    }

    fun searchFavoritesStations (query: String){
        val stations = ArrayList<Station>()
        AppData.stations.forEach {
            if (it.isFavorite&&it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        stationsApi.postValue(stations)
    }

    fun clearSearchStations (){
        stationsApi.postValue(AppData.stations)
    }

    fun searchGenres (query: String){
        val genresSearch = ArrayList<Genre>()
        AppData.genres.forEach {
            if (it.name.lowercase().contains(query.lowercase()))
                genresSearch.add(it)
        }
        genresSearch.sortBy { it.name }
        genresApi.postValue(genresSearch)
    }

    fun clearSearchGenres (){
        genresApi.postValue(AppData.genres)
    }

    fun searchCities (query: String){
        val citiesSearch = ArrayList<City>()
        AppData.cities.forEach {
            if (it.name.lowercase().contains(query.lowercase()))
                citiesSearch.add(it)
        }
        citiesSearch.sortBy { it.name }
        citiesApi.postValue(citiesSearch)
    }

    fun clearSearchCities (){
        citiesApi.postValue(AppData.cities)
    }

    fun updateStation(context: Context, station: Station){
        if (station.id==this.station.value!!.id){
            this.station.value = station
        }
        if (stations.value!!.contains(station))
            stations.value!![stations.value!!.indexOf(station)].isFavorite = station.isFavorite
        val favorites = HashSet<String>()
        AppData.stations.forEach {
            if (it.isFavorite){
                favorites.add(it.id.toString())
            }
        }
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .edit().putStringSet("favorites", favorites).apply()
        AppData.getFavorites(context)
        stationsApi.value = stations.value
    }

    fun setViewedStation(context: Context, station: Station){
        AppData.getStationById(station.id).isViewed = true
        val viewed = HashSet<String>()
        AppData.stations.forEach {
            if (it.isViewed){
                viewed.add(it.id.toString())
            }
        }
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .edit().putStringSet("viewed", viewed).apply()
        AppData.getViewed(context)
    }
}