package ru.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import ru.topradio.R
import ru.topradio.databinding.FragmentSettingsBinding
import ru.topradio.ui.dialogs.DialogSeekbar
import ru.topradio.ui.dialogs.DialogViewType
import ru.topradio.util.AppData

class SettingsFragment: Fragment(), DialogSeekbar.OnSeekBarChange,
    DialogViewType.OnDialogViewClick {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        binding.viewModel = (activity as MainActivity).viewModel
        binding.lifecycleOwner = this
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
            //if ((activity as MainActivity).player.isPlaying){
                (activity as MainActivity).player.setHandleAudioBecomingNoisy(isChecked)
            //}
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


        binding.viewModel!!.timerValue.value = AppData.getSettingInt(requireContext(),"timer")
        binding.settingTimer.setOnClickListener {
            val dialog = DialogSeekbar(requireContext(),this)
            dialog.setProgress(binding.viewModel!!.timerValue.value!!/10)
//            dialog.setProgress(binding.viewModel!!.timerValue.value!!)
            dialog.show()
            dialog.setOnDismissListener {
                AppData.setSettingInt(requireContext(), "timer",
                    binding.viewModel!!.timerValue.value!!)
                if (binding.viewModel!!.timerValue.value!!>0) {
                    if ((activity as MainActivity).viewModel.station.value!!.name != "") {
                        (activity as MainActivity).service.setTimerOff()
                    }
                }
            }
        }

        binding.viewModel!!.viewTypeValue.value =
            AppData.getSettingString(requireContext(),"view")
        binding.settingView.setOnClickListener {
            DialogViewType(requireContext(), this).show()
        }
    }

    override fun onSeekbarChanged(value: Int) {
        binding.viewModel!!.timerValue.value = value
    }

    override fun onViewTypeClick(viewType: String) {
        binding.viewModel!!.viewTypeValue.value = viewType
        AppData.setSettingString(requireContext(), "view",
            binding.viewModel!!.viewTypeValue.value!!)
    }


}