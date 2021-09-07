package com.app.topradio.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.DatePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.topradio.R
import com.app.topradio.databinding.FragmentAlarmBinding
import com.app.topradio.model.AlarmViewModel
import com.app.topradio.model.MainViewModel
import com.app.topradio.model.Station
import com.app.topradio.ui.adapters.DayAdapter
import com.app.topradio.util.AppData
import java.util.*

class AlarmFragment: Fragment(), DialogStations.OnDialogStationClick {

    private lateinit var binding: FragmentAlarmBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AlarmViewModel::class.java) }
    private val mainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchAlarm.isChecked = viewModel.getAlarmSetting(requireContext())
        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveAlarmSetting(requireContext(), isChecked)
        }

        viewModel.getDateTime(requireContext())

        binding.alarmDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog (requireContext(),
                { _, year, month, dayOfMonth ->
                    cal.set(year,month,dayOfMonth)
                    viewModel.setDate(requireContext(), cal.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
            dialog.show()
        }

        binding.timePicker.setIs24HourView(true)
        binding.timePicker.currentHour = viewModel.getHour(requireContext())
        binding.timePicker.currentMinute = viewModel.getMinute(requireContext())
        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            viewModel.saveTimeSetting(requireContext(), hourOfDay, minute)
        }

        binding.recyclerDays.adapter = DayAdapter(viewModel.days)

        binding.stationValue.text = AppData.getSettingString(requireContext(),"station")
        binding.alarmStation.setOnClickListener {
            mainViewModel.stations.observe(viewLifecycleOwner,{
                if (it!=null){
                    DialogStations(requireContext(), it, this).show()
                    mainViewModel.stations.removeObservers(this)
                }
            })
        }

        mainViewModel.getAllStations()

        binding.switchVolume.isChecked = viewModel.getVolumeSetting(requireContext())
        binding.switchVolume.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveVolumeSetting(requireContext(), isChecked)
        }
    }

    override fun onStationSelected(station: Station) {
        AppData.setSettingString(requireContext(),"station",station.name)
        AppData.setSettingInt(requireContext(),"stationId",station.id)
        binding.stationValue.text = station.name
    }
}