package ru.topradio.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioManager.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ru.topradio.R
import ru.topradio.model.Alarm
import ru.topradio.model.Station
import ru.topradio.ui.MainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class PlayerService: Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    var station = Station()
    var alarm = Alarm()
    var stopped = false
    var bitrateIndex = 0
    private lateinit var fileOutputStream: FileOutputStream
    val handler = Handler(Looper.getMainLooper())
    private val audioManager by lazy {getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    private var defaultVolume = -1
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder {
        return PlayerServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
//        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
//            15000,
//            50000,
//            15000,
//            1000)
//            .build()
        player = SimpleExoPlayer.Builder(this)
//            .setLoadControl(loadControl)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra("bundle")?.let{
            station = it.getSerializable("station") as Station
            val fromAlarm = it.getBoolean("fromAlarm")
            if (fromAlarm) {
                wakeLock =
                    (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerService::lock").apply {
                            acquire(5000)
                        }
                    }
                alarm = it.getSerializable("alarm") as Alarm
                checkAlarm()
            }
            bitrateIndex = 0
            station.bitrates.forEach { br ->
                if (br.isSelected) {
                    bitrateIndex = station.bitrates.indexOf(br)
                    return@forEach
                }
            }
            player.addMetadataOutput { metadata ->
                for (n in 0 until metadata.length()) {
                    when (val md = metadata[n]) {
                        is com.google.android.exoplayer2.metadata.icy.IcyInfo -> {
                            station.track = md.title?:""
                            LocalBroadcastManager.getInstance(this@PlayerService)
                                .sendBroadcast(Intent("player_track_name").apply {
                                    putExtra("track_name", station.track)
                                })
                        }
                        else -> {
                            station.track = ""
                        }
                    }
                }
            }
            player.addListener(object: Player.Listener{
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    station.isPlaying = isPlaying
                    if (!isPlaying) {
                        stopped = true
                        stopForeground(false)
                        if (station.isRecording) {
                            station.isRecording = false
                            LocalBroadcastManager.getInstance(this@PlayerService)
                                .sendBroadcast(Intent("player_stop_record"))
                        }
                    } else {
                        station.bitrates.forEach { bitrate -> bitrate.isSelected = false }
                        station.bitrates[bitrateIndex].isSelected = true
                        stopped = false
                        player.setHandleAudioBecomingNoisy(
                            AppData
                                .getSettingBoolean(this@PlayerService, "headphone")
                        )
                    }
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_state_changed").apply {
                            putExtra("isPlaying", isPlaying)
                        })
                }

                override fun onPlayerError(error: PlaybackException) {
                    handlePlayerError()
                }
            })
            playerNotificationManager = PlayerNotificationManager.Builder(this,
                1212, getString(R.string.app_name))
                .setMediaDescriptionAdapter(object:
                    PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        return station.name
                    }

                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        val intent1 = Intent(this@PlayerService, MainActivity::class.java)
                        val serviceBundle = Bundle()
                        serviceBundle.putSerializable("station", station)
                        intent1.putExtra("bundle", serviceBundle)
                        return PendingIntent.getActivity(
                            applicationContext, 0,
                            intent1, 0
                        )
                    }

                    override fun getCurrentContentText(player: Player): CharSequence {
                        return station.track
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        loadBitmap(station.icon, callback)
                        return null
                    }

                })
                .setNotificationListener(object:PlayerNotificationManager.NotificationListener{
                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean) {
                        station = Station()
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_close"))
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_stop_record"))
                        stopSelf()
                    }

                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean) {
                        if (!stopped) startForeground(notificationId, notification)
                    }
                })
                .build()
            playerNotificationManager.apply {
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setSmallIcon(R.drawable.ic_radio)
                setUseStopAction(true)
                setPriority(NotificationCompat.PRIORITY_MAX)
                setPlayer(player)
                setUseNextAction(false)
                setUsePreviousAction(false)
            }

            if (fromAlarm) {
                setVolume()
            }

            val audioAttr = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(audioAttr, true)
            player.playWhenReady = true
            if (station.bitrates.size>0) {
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                        .build()
                )
                player.prepare()
                if (AppData.getSettingInt(this, "timer") > 0) {
                    setTimerOff()
                }
            } else stopSelf()
        }

        return START_STICKY
    }

    private fun setVolume() {
        if (AppData.getSettingBoolean(this,"volume")){
            defaultVolume = audioManager.getStreamVolume(STREAM_MUSIC)
            audioManager.setStreamVolume(STREAM_MUSIC, audioManager.getStreamVolume(STREAM_ALARM),
                FLAG_PLAY_SOUND)
            player.volume = 0f
            val r: Runnable = object : Runnable {
                override fun run() {
                    handler.postDelayed(this, 2000)
                    if (player.volume<1f) {
                        player.volume += 0.01f
                    } else {
                        audioManager.setStreamVolume(STREAM_MUSIC, defaultVolume,
                            FLAG_PLAY_SOUND)
                        handler.removeCallbacksAndMessages(null)
                    }
                }
            }
            r.run()
        } else {
            audioManager.setStreamVolume(STREAM_MUSIC, audioManager.getStreamVolume(STREAM_ALARM),
                FLAG_PLAY_SOUND)
        }
    }

    private fun checkAlarm() {
        stopService(Intent(this, AlarmService::class.java))
        if (alarm.repeat.size>0){
            val cal = Calendar.getInstance()
            cal.timeInMillis = alarm.dateTime
            cal.add(Calendar.DATE,1)
            while (!alarm.repeat.contains("${cal.get(Calendar.DAY_OF_WEEK)}")){
                cal.add(Calendar.DATE,1)
            }
            alarm.dateTime = cal.timeInMillis
            val intent = Intent(this, AlarmService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("alarm", alarm)
            intent.putExtra("setAlarm", serviceBundle)
            startService(intent)
        }
    }

    fun setTimerOff() {
        val timer = Timer()
        val hourlyTask: TimerTask = object : TimerTask() {
            override fun run() {
                CoroutineScope(Dispatchers.Main).launch {
                    AppData.setSettingInt(this@PlayerService, "timer",0)
                    player.stop()
                    stopped = true
                    station = Station()
                    playerNotificationManager.setPlayer(null)
                    stopForeground(true)
                    stopSelf()
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_close"))
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_stop_record"))
                }
            }
        }
        timer.schedule (hourlyTask, AppData.getSettingInt(this,"timer")*60*1000L)
    }


    private fun handlePlayerError() {
        Log.v("DASD", "${isInternetAvailable()}")
        if (isInternetAvailable()) {
            bitrateIndex++
            if (bitrateIndex < station.bitrates.size) {
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                        .build()
                )
                player.prepare()
            } else {
                bitrateIndex = 0
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_state_changed").apply {
                        putExtra("isPlaying", false)
                    })
            }
        } else {
            if (AppData.getSettingBoolean(this@PlayerService,"reconnect")) {
                applicationContext.registerReceiver(mConnReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            } else {
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_close"))
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("no_internet"))
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_stop_record"))
                player.stop()
                stopped = true
                station = Station()
                playerNotificationManager.setPlayer(null)
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun loadBitmap(url: String, callback: PlayerNotificationManager.BitmapCallback?) {
        Glide.with(this)
            .asBitmap()
            .load("https://top-radio.ru/assets/image/radio/180/$url")
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    callback?.onBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }

    fun recordAudio(){
        station.isRecording = true
        startTimerRecord()
        Toast.makeText(this@PlayerService, R.string.start_record, Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlPath = URL(station.bitrates[bitrateIndex].url)
                val folder = File(Environment.getExternalStorageDirectory(), "TopRadio")
                val fileAudio = File(folder, "${station.name}_${Calendar.getInstance().timeInMillis}.mp3")
                if (!folder.exists()) folder.mkdirs()
                val inputStream = urlPath.openStream()
                fileOutputStream = FileOutputStream(fileAudio)
                var c: Int
                while (inputStream.read().also { c = it } != -1) {
                    fileOutputStream.write(c)
                    c++
                }
            } catch (e:Exception) {e.printStackTrace()}
        }
    }

    fun startTimerRecord(){
        val startTime = System.currentTimeMillis()
        val r: Runnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, 1000)
                val time = SimpleDateFormat("mm:ss", Locale.getDefault())
                    .format(System.currentTimeMillis()-startTime)
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_record_time").apply {
                        putExtra("time", time)
                    })
            }
        }
        r.run()
    }

    fun stopRecord(){
        handler.removeCallbacksAndMessages(null)
        station.isRecording = false
        try {
            fileOutputStream.close()
        } catch (e:java.lang.Exception){e.printStackTrace()}
        //Toast.makeText(this@PlayerService, R.string.stop_record, Toast.LENGTH_SHORT).show()
    }

    fun setBitrate(index: Int){
        bitrateIndex = index
        if (bitrateIndex>=station.bitrates.size) bitrateIndex = 0
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                .build())
        player.prepare()
    }

    @Throws(InterruptedException::class, IOException::class)
    fun isInternetAvailable(): Boolean {
        val command = "ping -c 1 google.com"
        return Runtime.getRuntime().exec(command).waitFor() == 0
    }

    inner class PlayerServiceBinder : Binder() {
        val service
            get() = this@PlayerService

        val player
            get() = this@PlayerService.player

        val station get() = this@PlayerService.station
    }

    private val mConnReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val currentNetworkInfo =
                intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
            if (currentNetworkInfo!!.isConnected) {
                player.prepare()
                applicationContext.unregisterReceiver(this)
            }
        }
    }

    override fun onDestroy() {
        if (defaultVolume>-1)
            audioManager.setStreamVolume(STREAM_MUSIC, defaultVolume,
                FLAG_PLAY_SOUND)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}