package com.app.topradio.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.app.topradio.R
import com.app.topradio.databinding.DialogStationsBinding
import com.app.topradio.model.Station
import com.app.topradio.ui.adapters.StationsDialogListAdapter

class DialogStations(context:Context, private val stations: ArrayList<Station>,
                     private val listener: OnDialogStationClick):
    Dialog(context, R.style.Theme_Dialog), StationsDialogListAdapter.OnClickListener {

    private val binding by lazy { DataBindingUtil
        .inflate<DialogStationsBinding>(LayoutInflater.from(context),
            R.layout.dialog_stations,null,false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        binding.adapter = StationsDialogListAdapter(this).apply {
            submitList(stations)
        }
    }

    interface OnDialogStationClick{
        fun onStationSelected(station: Station)
    }

    override fun onStationClick(station: Station) {
        listener.onStationSelected(station)
        dismiss()
    }
}