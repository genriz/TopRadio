package com.app.topradio.model

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.topradio.util.AppData
import com.app.topradio.api.ApiRadio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel: ViewModel() {
    val stationsApi = MutableLiveData<ArrayList<Station>>()
    val stations: LiveData<ArrayList<Station>> = stationsApi
    val stationsFavoritesApp = MutableLiveData<ArrayList<Station>>()
    val stationsFavorites: LiveData<ArrayList<Station>> = stationsFavoritesApp
    val station = MutableLiveData<Station>().apply { value = Station() }
    val stationPager = MutableLiveData<Station>().apply { value = Station() }
    private var job: Job? = null
    private val genresApi = MutableLiveData<ArrayList<Genre>>().apply {
        AppData.genres.forEach { genre ->
            var count = 0
            AppData.stations.forEach { station ->
                if (station.genres.contains(genre.id)) count++
            }
            genre.count = count
        }
        AppData.genres.sortByDescending { it.count }
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
        AppData.cities.sortByDescending { it.count }
        value = AppData.cities
    }
    val cities: LiveData<ArrayList<City>> = citiesApi
    val updateItemPosition = MutableLiveData<Int>()
    val playerWaiting = MutableLiveData<Boolean>()
    val playerRecording = MutableLiveData<Boolean>().apply { value = false }
    val recordTime = MutableLiveData<String>()
    val playlistApi = MutableLiveData<ArrayList<PlaylistItem>>()
    val playlist: LiveData<ArrayList<PlaylistItem>> = playlistApi
    val timerValue = MutableLiveData<Int>()

    fun getAllStations(){
        AppData.stations.forEach { station ->
            if (AppData.favorites.contains(station.id.toString()))
                station.isFavorite = true
        }
        stationsApi.postValue(AppData.stations)
    }

    fun getFavoriteStations(){
        val favorites = ArrayList<Station>()
        AppData.stations.forEach {station->
            if (station.isFavorite) favorites.add(station)
        }
        stationsFavoritesApp.postValue(favorites)
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

    fun getStationById(station: Station): Station?{
        return if (stations.value!=null) {
            var stationById = Station()
            stations.value?.forEach { station_ ->
                if (station_.id == station.id) {
                    stationById = station_
                }
            }
            stationById
        } else null
    }

    fun getStationPosition(station: Station): Int{
        return if (stations.value!=null){
            var position = 0
            stations.value?.forEach {station_ ->
                if (station_.id == station.id) {
                    position = stations.value!!.indexOf(station_)
                }
            }
            position
        } else 0
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
        stationsFavoritesApp.postValue(stations)
    }

    fun clearSearchStations (){
        stationsApi.postValue(AppData.stations)
    }

    fun clearSearchStationsFavorites (){
        stationsFavoritesApp.postValue(AppData.stations)
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

    fun updateStationFavorite(context: Context, station: Station){
        if (station.id==this.station.value!!.id){
            this.station.value = station
        }
        if (station.isFavorite)
            AppData.favorites.add(station.id.toString())
        else AppData.favorites.remove(station.id.toString())
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .edit().putStringSet("favorites", AppData.favorites).apply()
        getFavoriteStations()
    }

    fun setViewedStation(context: Context, station: Station){
        if (station.isViewed)
            AppData.viewed.add(station.id.toString())
        else AppData.viewed.remove(station.id.toString())
        context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .edit().putStringSet("viewed", AppData.viewed).apply()
    }

    fun getTop100(station: String){
        job = CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi()
                .getTop100("https://playlists8.auto-messenger.ru/storage/playlists/" +
                        "$station/top-songs/100.json",
                    AppData.auth)
            if (response.isSuccessful&&response.body()!=null){
                val array = response.body()!!
                array.sortByDescending { it.total }
                playlistApi.postValue(array)
            }
        }
    }

    fun getPlaylist(station: String, date: String){
        job = CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi()
                .getTop100("https://playlists8.auto-messenger.ru/storage/playlists/" +
                        "$station/$date/index.json",
                    AppData.auth)
            if (response.isSuccessful&&response.body()!=null){
                val array = response.body()!!
                array.sortByDescending { it.start_at }
                playlistApi.postValue(array)
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
        job = null
        super.onCleared()
    }

}