package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import androidx.databinding.DataBindingUtil
import ru.topradio.R
import ru.topradio.databinding.TopMenuLayoutBinding

class DialogMenu(context:Context, private val listener: OnDialogMenuClick):
    Dialog(context, R.style.Menu_Dialog) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil
            .inflate<TopMenuLayoutBinding>(LayoutInflater.from(context),
                R.layout.top_menu_layout,null,false)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.TOP or Gravity.END)
            attributes.x = 12
            attributes.y = 120
        }

        binding.menu1.setOnClickListener {
            listener.onMenuPositionClick(0)
            dismiss()
        }
        binding.menu2.setOnClickListener {
            listener.onMenuPositionClick(1)
            dismiss()
        }
        binding.menu3.setOnClickListener {
            listener.onMenuPositionClick(2)
            dismiss()
        }
        binding.menu4.setOnClickListener {
            listener.onMenuPositionClick(3)
            dismiss()
        }
        binding.menu5.setOnClickListener {
            listener.onMenuPositionClick(4)
            dismiss()
        }

    }

    interface OnDialogMenuClick{
        fun onMenuPositionClick(position: Int)
    }
}