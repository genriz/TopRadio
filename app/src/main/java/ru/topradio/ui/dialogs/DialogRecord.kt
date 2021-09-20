package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import ru.topradio.R

class DialogRecord(context:Context, private val listener: OnClick): Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_record)

        val path = Environment.getExternalStorageDirectory().path + "/TopRadio"
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