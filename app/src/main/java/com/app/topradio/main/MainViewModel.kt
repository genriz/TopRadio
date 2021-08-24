package com.app.topradio.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.topradio.AppData
import com.app.topradio.api.ApiRadio
import com.app.topradio.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel: ViewModel() {
    private val _stations = MutableLiveData<ArrayList<Station>>()
    val stations: LiveData<ArrayList<Station>> = _stations
    val station = MutableLiveData<Station>().apply { value = Station() }
    private val allStations = ArrayList<Station>()

    fun getStations(){
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getRadios(AppData.auth)
            if (response.isSuccessful){
                allStations.clear()
                allStations.addAll(response.body()!!)
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
        _stations.postValue(stations)
    }

    fun clearSearch (){
        _stations.postValue(allStations)
    }
}