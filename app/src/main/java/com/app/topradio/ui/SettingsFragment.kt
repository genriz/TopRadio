package com.app.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentSettingsBinding
import com.app.topradio.util.AppData

class SettingsFragment: Fragment(){

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchAutoPlay.isChecked = AppData
            .getSettingBoolean(requireContext(),"autoplay")
        binding.switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            AppData.setSettingBoolean(requireContext(), "autoplay", isChecked)
        }

        binding.switchHeadphone.isChecked = AppData
            .getSettingBoolean(requireContext(),"headphone")
        binding.switchHeadphone.setOnCheckedChangeListener { _, isChecked ->
            AppData.setSettingBoolean(requireContext(), "headphone", isChecked)
            if ((activity as MainActivity).player.isPlaying){
                (activity as MainActivity).player.setHandleAudioBecomingNoisy(isChecked)
            }
        }

        binding.switchReconnect.isChecked = AppData
            .getSettingBoolean(requireContext(),"reconnect")
        binding.switchReconnect.setOnCheckedChangeListener { _, isChecked ->
            AppData.setSettingBoolean(requireContext(), "reconnect", isChecked)
        }

        binding.switchDarkTheme.isChecked = AppData.getSettingBoolean(requireContext(),"theme")
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            AppData.setSettingBoolean(requireContext(), "theme", isChecked)
            if (isChecked)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

//        val bufferSetting = AppData.getSettingInt(requireContext(),"buffer")
//        val buffer = if (AppData.bufferSizes[bufferSetting]<1000)
//            "${AppData.bufferSizes[bufferSetting]} ${requireContext().getString(R.string.ms)}"
//        else "${AppData.bufferSizes[bufferSetting]/1000} ${requireContext().getString(R.string.sec)}"
//        binding.bufferSize.text = buffer

        val timerSetting = AppData.getSettingInt(requireContext(),"timer")
        val timer = if (AppData.timerValues[timerSetting]==0)
            requireContext().getString(R.string.off)
        else "${AppData.timerValues[timerSetting]} ${requireContext().getString(R.string.min)}"
        binding.timerValue.text = timer
        binding.settingTimer.setOnClickListener {
            
        }
    }


}