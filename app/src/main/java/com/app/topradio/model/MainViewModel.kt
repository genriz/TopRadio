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
    val stationsApi = MutableLiveData<ArrayList<Station>>()
    val stations: LiveData<ArrayList<Station>> = stationsApi
    val station = MutableLiveData<Station>().apply { value = Station() }
    private val allStations = ArrayList<Station>()
    var favorites = HashSet<String>()

    private val genresApi = MutableLiveData<ArrayList<Genre>>()
    val genres: LiveData<ArrayList<Genre>> = genresApi

    private val citiesApi = MutableLiveData<ArrayList<City>>()
    val cities: LiveData<ArrayList<City>> = citiesApi

    fun getStations(){
        allStations.clear()
        allStations.addAll(AppData.stations)
        allStations.forEach {
            if (favorites.contains(it.id.toString())) it.isFavorite = true
        }
        allStations.sortBy { it.position }
        stationsApi.postValue(allStations)
    }

    fun searchStations (query: String){
        val stations = ArrayList<Station>()
        allStations.forEach {
            if (it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        stationsApi.postValue(stations)
    }

    fun searchFavoritesStations (query: String){
        val stations = ArrayList<Station>()
        allStations.forEach {
            if (it.isFavorite&&it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        stationsApi.postValue(stations)
    }

    fun clearSearch (){
        stationsApi.postValue(allStations)
    }

    fun getGenres(){
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getGenres(AppData.auth)
            if (response.isSuccessful){
                genresApi.postValue(response.body())
            }
        }
    }

    fun getCities(){
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getCities(AppData.auth)
            if (response.isSuccessful){
                citiesApi.postValue(response.body())
            }
        }
    }
}