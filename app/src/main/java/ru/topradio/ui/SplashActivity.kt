package ru.topradio.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import ru.topradio.R
import ru.topradio.api.ApiRadio
import ru.topradio.ui.dialogs.DialogInternet
import ru.topradio.util.AppData
import kotlinx.coroutines.*
import com.yandex.mobile.ads.common.MobileAds


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var error = ""
    private val api = ApiRadio()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        try {
            if (AppData.getSettingBoolean(this, "theme"))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } catch (e:Exception){}

        getApiData()

    }

    private fun getApiData(){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getApi().getRadios(AppData.auth)
                if (response.isSuccessful && response.body() != null) {
                    AppData.stations.clear()
                    response.body()!!.forEach { station ->
                        if (station.bitrates.size > 0) {
                            station.bitrates[0].isSelected = true
                            AppData.stations.add(station)
                        }
                    }
                    try {
                        val response2 = api.getApi().getGenres(AppData.auth)
                        if (response2.isSuccessful && response2.body() != null) {
                            AppData.genres.clear()
                            AppData.genres.addAll(response2.body()!!)
                            AppData.genres.forEach { genre ->
                                var count = 0
                                AppData.stations.forEach { station ->
                                    if (station.genres.contains(genre.id)) count++
                                }
                                genre.count = count
                            }
                            AppData.genres.sortByDescending { it.count }
                            try {
                                val response3 =
                                    api.getApi().getCities(AppData.auth)
                                if (response3.isSuccessful && response3.body() != null) {
                                    AppData.cities.clear()
                                    AppData.cities.addAll(response3.body()!!)
                                    AppData.cities.forEach { city ->
                                        var count = 0
                                        AppData.stations.forEach { station ->
                                            if (station.cities.contains(city.id)) count++
                                        }
                                        city.count = count
                                    }
                                    AppData.cities.sortByDescending { it.count }
                                    AppData.getFavorites(this@SplashActivity)
                                    MobileAds.initialize(this@SplashActivity) {
                                        startActivity(
                                            Intent(
                                                this@SplashActivity,
                                                MainActivity::class.java
                                            )
                                        )
                                        finish()
                                    }
                                } else {
                                    error = "api error: cities"
                                    handleException()
                                }
                            } catch (e: Exception) {
                                error = e.toString()
                                handleException()
                            }
                        } else {
                            error = "api error: genres"
                            handleException()
                        }
                    } catch (e: Exception) {
                        error = e.toString()
                        handleException()
                    }
                } else {
                    error = "api error: stations"
                    handleException()
                }
            } catch (e: Exception) {
                error = e.toString()
                handleException()
            }
        }
    }

    @UiThread
    private fun handleException(){
        try {
            api.client.dispatcher.cancelAll()
            if (!isDestroyed) {
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.Main) {
                        DialogInternet(this@SplashActivity).apply {
                            setOnDismissListener {
                                getApiData()
                            }
                        }.show()
                        Toast.makeText(this@SplashActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e:Exception){e.printStackTrace()}
    }

    override fun onDestroy() {
        api.client.dispatcher.cancelAll()
        super.onDestroy()
    }
}