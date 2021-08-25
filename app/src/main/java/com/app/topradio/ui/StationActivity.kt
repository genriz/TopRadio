package com.app.topradio.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.app.topradio.R
import com.app.topradio.databinding.ActivityStationBinding

class StationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_station)
    }
}