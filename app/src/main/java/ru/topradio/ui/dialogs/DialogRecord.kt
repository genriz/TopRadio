package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import ru.topradio.R
import java.io.File

class DialogRecord(context:Context, private val listener: OnClick): Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_record)

        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Environment.DIRECTORY_MUSIC + File.separator + "TopRadio"
        else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            .toString() + File.separator + "TopRadio"

        findViewById<TextView>(R.id.dialog_text2).text = path

        findViewById<TextView>(R.id.txtOpen).setOnClickListener {
            listener.openFolder()
            dismiss()
        }

        findViewById<TextView>(R.id.txtClose).setOnClickListener {
            dismiss()
        }
    }

    interface OnClick{
        fun openFolder()
    }
}