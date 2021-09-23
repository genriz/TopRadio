package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import ru.topradio.R

class DialogStationOff(context:Context): Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_station_off)

        findViewById<TextView>(R.id.txtClose).setOnClickListener {
            dismiss()
        }
    }

}