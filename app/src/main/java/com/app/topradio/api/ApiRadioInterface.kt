package com.app.topradio.api

import com.app.topradio.model.City
import com.app.topradio.model.Genre
import com.app.topradio.model.PlaylistItem
import com.app.topradio.model.Station
import retrofit2.Response
import retrofit2.http.*


interface ApiRadioInterface {

    @GET("radios.json")
    suspend fun getRadios(@Header("Authorization") auth: String): Response<ArrayList<Station>>

    @GET("cities.json")
    suspend fun getCities(@Header("Authorization") auth: String): Response<ArrayList<City>>

    @GET("genres.json")
    suspend fun getGenres(@Header("Authorization") auth: String): Response<ArrayList<Genre>>

    @GET
    suspend fun getTop100(@Url path: String,
                          @Header("Authorization") auth: String): Response<ArrayList<PlaylistItem>>

}