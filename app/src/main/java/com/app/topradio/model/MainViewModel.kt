package com.app.topradio.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.topradio.util.AppData
import com.app.topradio.api.ApiRadio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class MainViewModel: ViewModel() {
    val stationsApi = MutableLiveData<ArrayList<Station>>().apply {
        value = AppData.stations
    }
    val stations: LiveData<ArrayList<Station>> = stationsApi
    val station = MutableLiveData<Station>().apply { value = Station() }

    private val genresApi = MutableLiveData<ArrayList<Genre>>().apply {
        value = AppData.genres
    }
    val genres: LiveData<ArrayList<Genre>> = genresApi
    private val citiesApi = MutableLiveData<ArrayList<City>>().apply {
        value = AppData.cities
    }
    val cities: LiveData<ArrayList<City>> = citiesApi

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
}