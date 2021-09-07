package com.app.topradio.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import androidx.databinding.DataBindingUtil
import com.app.topradio.R
import com.app.topradio.databinding.DialogViewTypeBinding

class DialogViewType(context:Context, private val listener: OnDialogViewClick):
    Dialog(context, R.style.Theme_Dialog) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil
            .inflate<DialogViewTypeBinding>(LayoutInflater.from(context),
                R.layout.dialog_view_type,null,false)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.viewList.setOnClickListener {
            listener.onViewTypeClick(context.getString(R.string.list))
            dismiss()
        }

        binding.viewGrid.setOnClickListener {
            listener.onViewTypeClick(context.getString(R.string.grid))
            dismiss()
        }

    }

    interface OnDialogViewClick{
        fun onViewTypeClick(viewType: String)
    }
}