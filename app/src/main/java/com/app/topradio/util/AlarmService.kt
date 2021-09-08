package com.app.topradio.util

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.app.topradio.R
import com.app.topradio.model.Alarm
import com.app.topradio.model.Station
import com.app.topradio.ui.MainActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.text.SimpleDateFormat
import java.util.*

class AlarmService: Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManagerAlarm: PlayerNotificationManager
    var station = Station()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(this)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra("setAlarm")?.let{
            val alarm = it.getSerializable("alarm") as Alarm
            setAlarm(alarm)
        }
        return START_STICKY
    }

    private fun setAlarm(alarm: Alarm){
        playerNotificationManagerAlarm = PlayerNotificationManager.Builder(this,
            1213, getString(R.string.app_name))
            .setMediaDescriptionAdapter(object:
                PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return "Alarm on with ${alarm.station.name}"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent1 = Intent(this@AlarmService, MainActivity::class.java)
                    val serviceBundle = Bundle()
                    serviceBundle.putSerializable("station", alarm.station)
                    intent1.putExtra("bundle", serviceBundle)
                    return PendingIntent.getActivity(
                        this@AlarmService, 0,
                        intent1, 0
                    )
                }

                override fun getCurrentContentText(player: Player): CharSequence {
                    return SimpleDateFormat("HH:mm dd-MM-yy", Locale.getDefault())
                        .format(alarm.dateTime)
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    return null
                }

            })
            .setNotificationListener(object:PlayerNotificationManager.NotificationListener{
                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean) {
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean) {
                    startForeground(notificationId, notification)
                }
            })
            .build()
        val player = SimpleExoPlayer.Builder(this).build()
        playerNotificationManagerAlarm.apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_radio)
            setUseStopAction(false)
            setUsePlayPauseActions(false)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setPriority(NotificationCompat.PRIORITY_MAX)
            setPlayer(player)
        }
        player.playWhenReady = true
        player.setMediaItem(
            MediaItem.Builder()
                .setUri("")
                .build())
        player.prepare()

        val intent = Intent(this, PlayerService::class.java)
        val serviceBundle = Bundle()
        serviceBundle.putSerializable("station", alarm.station)
        serviceBundle.putSerializable("alarm", alarm)
        serviceBundle.putBoolean("fromAlarm", true)
        intent.putExtra("bundle", serviceBundle)

        (getSystemService(ALARM_SERVICE) as AlarmManager).setExact(
            AlarmManager.RTC_WAKEUP,
            alarm.dateTime,
            PendingIntent.getService(
                applicationContext, 0,
                intent, 0
            )
        )
    }

    override fun onDestroy() {
        AppData.setSettingBoolean(this,"alarm",false)
        super.onDestroy()
    }
}