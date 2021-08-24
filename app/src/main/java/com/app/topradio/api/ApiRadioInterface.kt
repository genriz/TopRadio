package com.app.topradio.api

import com.app.topradio.model.Station
import retrofit2.Response
import retrofit2.http.*


interface ApiRadioInterface {

    @GET("radios.json")
    suspend fun getRadios(@Header("Authorization") auth: String): Response<ArrayList<Station>>


}