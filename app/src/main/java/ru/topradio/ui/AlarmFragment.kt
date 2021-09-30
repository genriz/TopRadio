package ru.topradio.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.topradio.R
import ru.topradio.databinding.FragmentAlarmBinding
import ru.topradio.model.Alarm
import ru.topradio.model.AlarmViewModel
import ru.topradio.model.MainViewModel
import ru.topradio.model.Station
import ru.topradio.ui.adapters.DayAdapter
import ru.topradio.ui.dialogs.DialogStations
import ru.topradio.util.AlarmService
import ru.topradio.util.AppData
import java.util.*
import kotlin.collections.ArrayList

class AlarmFragment: Fragment(), DialogStations.OnDialogStationClick, DayAdapter.OnClickListener {

    private lateinit var binding: FragmentAlarmBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AlarmViewModel::class.java) }
    private val mainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    private val dialog by lazy { DialogStations(requireContext(), ArrayList(), this) }
    private val cal = Calendar.getInstance()
    private var stationSelected = Station()
    private var repeatDays = HashSet<String>()

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
            if (isChecked) setAlarm()
            else requireContext().stopService(Intent(requireContext(), AlarmService::class.java))
        }

        viewModel.getDateTime(requireContext())
        cal.timeInMillis = viewModel.date.value!!

        binding.alarmDate.setOnClickListener {
            val dialog = DatePickerDialog (requireContext(),
                { _, year, month, dayOfMonth ->
                    cal.set(year,month,dayOfMonth)
                    viewModel.setDate(requireContext(), cal.timeInMillis)
                    if (binding.switchAlarm.isChecked)
                        binding.switchAlarm.isChecked = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
            dialog.show()
        }

        cal.set(Calendar.HOUR_OF_DAY, viewModel.getHour(requireContext()))
        cal.set(Calendar.MINUTE, viewModel.getMinute(requireContext()))
        binding.timePicker.setIs24HourView(true)
        binding.timePicker.currentHour = viewModel.getHour(requireContext())
        binding.timePicker.currentMinute = viewModel.getMinute(requireContext())
        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            viewModel.saveTimeSetting(requireContext(), hourOfDay, minute)
            if (binding.switchAlarm.isChecked)
                binding.switchAlarm.isChecked = false
        }

        repeatDays = AppData.getRepeatDays(requireContext())
        binding.recyclerDays.adapter = DayAdapter(viewModel.days, repeatDays, this)

        stationSelected = AppData.getStationById(AppData
            .getSettingInt(requireContext(),"stationId"))
        binding.stationValue.text = stationSelected.name
        binding.alarmStation.setOnClickListener {
            dialog.show()
            mainViewModel.stations.observe(viewLifecycleOwner,{
                if (it!=null){
                    dialog.updateList(it)
                }
            })
            dialog.setOnDismissListener {
                mainViewModel.stations.removeObservers(this)
                mainViewModel.clearSearchStations()
            }
        }

        mainViewModel.getAllStations()

        binding.switchVolume.isChecked = viewModel.getVolumeSetting(requireContext())
        binding.switchVolume.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveVolumeSetting(requireContext(), isChecked)
            if (binding.switchAlarm.isChecked) setAlarm()
        }
    }

    private fun setAlarm() {
        requireContext().stopService(Intent(requireContext(), AlarmService::class.java))
        cal.set(Calendar.SECOND,0)
        if (cal.timeInMillis>Calendar.getInstance().timeInMillis
            &&repeatDays.size==0) {
            val alarm = Alarm().apply {
                dateTime = cal.timeInMillis
                station = stationSelected
                repeat = repeatDays
            }
            val intent = Intent(requireContext(), AlarmService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("alarm", alarm)
            intent.putExtra("setAlarm", serviceBundle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } else if (repeatDays.size>0) {
            cal.set(Calendar.DAY_OF_MONTH,Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.MONTH,Calendar.getInstance().get(Calendar.MONTH))
            if (!repeatDays.contains("${cal.get(Calendar.DAY_OF_WEEK)}")) {
                cal.add(Calendar.DATE,1)
                while (!repeatDays.contains("${cal.get(Calendar.DAY_OF_WEEK)}")) {
                    cal.add(Calendar.DATE, 1)
                }
            }
            val alarm = Alarm().apply {
                dateTime = cal.timeInMillis
                station = stationSelected
                repeat = repeatDays
            }
            val intent = Intent(requireContext(), AlarmService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("alarm", alarm)
            intent.putExtra("setAlarm", serviceBundle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } else {
            binding.switchAlarm.isChecked = false
            Toast.makeText(requireContext(), R.string.alarm_wrong_time, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onStationSelected(station: Station) {
        stationSelected = station
        AppData.setSettingInt(requireContext(),"stationId",station.id)
        binding.stationValue.text = station.name
        if (binding.switchAlarm.isChecked) setAlarm()
    }

    override fun onSearch(query: String) {
        if (query.length>2)
            mainViewModel.searchStations(query)
        else mainViewModel.clearSearchStations()
    }

    override fun onDayClicked(position: Int, selected: Boolean) {
        if (selected) {
            repeatDays.add("${AppData.calDays[position]}")
        } else {
            repeatDays.remove("${AppData.calDays[position]}")
        }
        AppData.setRepeatDays(requireContext(), repeatDays)
        if (repeatDays.size==0) cal.timeInMillis = viewModel.date.value!!
        if (binding.switchAlarm.isChecked) setAlarm()
    }
}