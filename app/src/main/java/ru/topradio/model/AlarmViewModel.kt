package ru.topradio.model

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.topradio.util.AppData
import java.util.*
import kotlin.collections.ArrayList

class AlarmViewModel: ViewModel() {
    val date = MutableLiveData<Long>()
    val days = ArrayList<String>().apply {
        addAll(arrayListOf("П","В","С","Ч","П","С","В"))
    }

    fun getAlarmSetting(context: Context):Boolean{
        return AppData.getSettingBoolean(context,"alarm")
    }
    fun saveAlarmSetting(context: Context, isChecked: Boolean){
        AppData.setSettingBoolean(context, "alarm", isChecked)
    }

    fun getDateTime(context: Context){
        var date = AppData.getSettingLong(context,"date")
        if (date in 0..0) date = Calendar.getInstance().timeInMillis
        this.date.value = date
    }
    fun setDate(context: Context, date:Long){
        this.date.value = date
        AppData.setSettingLong(context, "date", date)
    }

    fun getHour(context: Context): Int{
        var hour = AppData.getSettingInt(context,"hour")
        if (hour in -1..-1) hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour
    }
    fun getMinute(context: Context): Int{
        var minute = AppData.getSettingInt(context,"minute")
        if (minute in -1..-1) minute = Calendar.getInstance().get(Calendar.MINUTE)
        return minute
    }

    fun getVolumeSetting(context: Context):Boolean{
        return AppData.getSettingBoolean(context,"volume")
    }
    fun saveVolumeSetting(context: Context, isChecked: Boolean){
        AppData.setSettingBoolean(context, "volume", isChecked)
    }

    fun saveTimeSetting(context: Context, hour: Int, minute: Int){
        AppData.setSettingInt(context, "hour", hour)
        AppData.setSettingInt(context, "minute", minute)
    }
}