package ru.topradio.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import ru.topradio.R
import ru.topradio.databinding.SeekbarBinding

class DialogSeekbar(context:Context, private val listener: OnSeekBarChange):
    Dialog(context, R.style.Theme_Dialog) {

    private val binding by lazy { DataBindingUtil
        .inflate<SeekbarBinding>(LayoutInflater.from(context),
            R.layout.seekbar,null,false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.seekBar1.progress = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.seekBar1.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.seekBar.progress = progress
                setTextProgress(progress, seekBar)
                listener.onSeekbarChanged(progress*10)
//                listener.onSeekbarChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

    }

    private fun setTextProgress(progress: Int, seekBar: SeekBar) {
        binding.label.text = (progress*10).toString()
//        binding.label.text = (progress).toString()
        val width = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
        val thumbPos = seekBar.paddingLeft + width * seekBar.progress / seekBar.max

        binding.label.measure(0, 0)
        val txtW: Int = binding.label.measuredWidth
        val delta = txtW / 2
        binding.label.x = seekBar.x + thumbPos - delta
        binding.label.y = seekBar.y + seekBar.thumb.intrinsicHeight/5
    }

    fun setProgress(value: Int){
        binding.seekBar1.progress = value
        binding.seekBar1.post {
            setTextProgress(value, binding.seekBar1)
        }
        binding.seekBar.progress = value

    }

    interface OnSeekBarChange{
        fun onSeekbarChanged(value: Int)
    }
}