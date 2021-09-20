package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import ru.topradio.R

class DialogInternet(context:Context): Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_internet)

        findViewById<TextView>(R.id.txtClose).setOnClickListener {
            dismiss()
        }
    }

}