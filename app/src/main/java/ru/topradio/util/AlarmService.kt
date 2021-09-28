package ru.topradio.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import ru.topradio.R
import ru.topradio.model.Alarm
import ru.topradio.model.Station
import ru.topradio.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmService: Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var alarmIntent: PendingIntent? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra("setAlarm")?.let{
            val alarm = it.getSerializable("alarm") as Alarm
            setAlarm(alarm)
        }
        return START_STICKY
    }

    private fun setAlarm(alarm: Alarm){
//        if (isServiceStarted) return
        isServiceStarted = true

        GlobalScope.launch(Dispatchers.IO) {
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmService2::lock").apply {
                        acquire()
                    }
                }
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor()
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    Log.v("DASD","service is running")
                    wakeLock?.let {
                        if (it.isHeld) {
                            it.release()
                        }
                    }
                }
                delay(300000L)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                getString(R.string.app_name),
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = getString(R.string.app_name)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent1 = Intent(this@AlarmService, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this@AlarmService,
            0, intent1, 0)

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, getString(R.string.app_name)
        ) else Notification.Builder(this)

        val notification = builder
            .setContentTitle("Alarm on with ${alarm.station.name}")
            .setContentText(SimpleDateFormat("HH:mm dd-MM-yy", Locale.getDefault())
                .format(alarm.dateTime))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_radio)
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()

        val intent = Intent(this, PlayerService::class.java)
        val serviceBundle = Bundle()
        serviceBundle.putSerializable("station", alarm.station)
        serviceBundle.putSerializable("alarm", alarm)
        serviceBundle.putBoolean("fromAlarm", true)
        intent.putExtra("bundle", serviceBundle)

        alarmIntent = PendingIntent.getService(
            applicationContext, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT)
        (getSystemService(ALARM_SERVICE) as AlarmManager).setExact(
            AlarmManager.RTC_WAKEUP,
            alarm.dateTime,
            alarmIntent!!)

        startForeground(1021, notification)
    }

    override fun onDestroy() {
        (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(alarmIntent!!)
        alarmIntent = null
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        isServiceStarted = false
        stopForeground(true)
        AppData.setSettingBoolean(this,"alarm",false)
        super.onDestroy()
    }
}