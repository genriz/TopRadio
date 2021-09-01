package com.app.topradio.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.app.topradio.R
import com.app.topradio.api.ApiRadio
import com.app.topradio.util.AppData
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        job = CoroutineScope(Dispatchers.IO).launch {
            val response = ApiRadio().getApi().getRadios(AppData.auth)
            if (response.isSuccessful&&response.body()!=null){
                AppData.stations.clear()
                AppData.stations.addAll(response.body()!!)
                AppData.stations.forEach { station->
                    station.bitrates[0].isSelected = true
                }
                job = CoroutineScope(Dispatchers.IO).launch {
                    val response2 = ApiRadio().getApi().getGenres(AppData.auth)
                    if (response2.isSuccessful&&response2.body()!=null){
                        AppData.genres.clear()
                        AppData.genres.addAll(response2.body()!!)
                        job = CoroutineScope(Dispatchers.IO).launch {
                            val response3 = ApiRadio().getApi().getCities(AppData.auth)
                            if (response3.isSuccessful&&response3.body()!=null){
                                AppData.cities.clear()
                                AppData.cities.addAll(response3.body()!!)
                                AppData.getFavorites(this@SplashActivity)
                                AppData.getViewed(this@SplashActivity)
                                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        super.onDestroy()
    }
}