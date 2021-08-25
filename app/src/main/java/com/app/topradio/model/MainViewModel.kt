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
    val _stations = MutableLiveData<ArrayList<Station>>()
    val stations: LiveData<ArrayList<Station>> = _stations
    val station = MutableLiveData<Station>().apply { value = Station() }
    private val allStations = ArrayList<Station>()
    var favorites = HashSet<String>()

    fun getStations(){
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getRadios(AppData.auth)
            if (response.isSuccessful){
                allStations.clear()
                allStations.addAll(response.body()!!)
                allStations.forEach {
                    if (favorites.contains(it.id.toString())) it.isFavorite = true
                }
                allStations.sortBy { it.position }
                _stations.postValue(allStations)
            }
        }
    }

    fun searchStations (query: String){
        val stations = ArrayList<Station>()
        allStations.forEach {
            if (it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        _stations.postValue(stations)
    }

    fun searchFavoritesStations (query: String){
        val stations = ArrayList<Station>()
        allStations.forEach {
            if (it.isFavorite&&it.name.lowercase().contains(query.lowercase()))
                stations.add(it)
        }
        stations.sortBy { it.position }
        _stations.postValue(stations)
    }

    fun clearSearch (){
        _stations.postValue(allStations)
    }
}