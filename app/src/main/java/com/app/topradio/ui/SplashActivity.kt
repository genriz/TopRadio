package com.app.topradio.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.topradio.R
import com.app.topradio.api.ApiRadio
import com.app.topradio.util.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getRadios(AppData.auth)
            if (response.isSuccessful&&response.body()!=null){
                AppData.stations.clear()
                AppData.stations.addAll(response.body()!!)
                CoroutineScope(Dispatchers.IO).launch {
                    val response2 = ApiRadio().getApi().getGenres(AppData.auth)
                    if (response2.isSuccessful&&response2.body()!=null){
                        AppData.genres.clear()
                        AppData.genres.addAll(response2.body()!!)
                        CoroutineScope(Dispatchers.IO).launch {
                            val response3 = ApiRadio().getApi().getCities(AppData.auth)
                            if (response3.isSuccessful&&response3.body()!=null){
                                AppData.cities.clear()
                                AppData.cities.addAll(response3.body()!!)
                                AppData.getFavorites(this@SplashActivity)
                                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}