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
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*
import ru.topradio.R
import ru.topradio.model.Alarm
import ru.topradio.model.Station
import ru.topradio.ui.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class PlayerService: Service() {

    private lateinit var player: SimpleExoPlayer
    private var playerNotificationManager: PlayerNotificationManager? = null
    var station = Station()
    var alarm = Alarm()
    var stopped = false
    var bitrateIndex = 0
    private lateinit var fileOutputStream: FileOutputStream
    val handler = Handler(Looper.getMainLooper())
    private val audioManager by lazy {getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    private var defaultVolume = -1
    private var wakeLock: PowerManager.WakeLock? = null
    private var fromAlarm = false

    override fun onBind(intent: Intent?): IBinder {
        return PlayerServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(this)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra("bundle")?.let{
            station = it.getSerializable("station") as Station
            fromAlarm = it.getBoolean("fromAlarm")
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
            setStations()
        }

        return START_STICKY
    }

    private fun setStations(){
        if (fromAlarm){
            AppData.stationsPlayer.clear()
            AppData.stationsPlayer.add(station)
            player.clearMediaItems()
            player.setMediaItem(MediaItem.Builder()
                .setUri(Uri.parse(station.bitrates[0].url))
                .build())
            player.prepare()
            handler.postDelayed({
                if (player.playbackState!=ExoPlayer.STATE_READY){
                    handler.removeCallbacksAndMessages(null)
                    exitService()
                }
            }, 5000)

        } else {
            if (AppData.stationsPlayer.size>0){
                val items = ArrayList<MediaItem>()
                AppData.stationsPlayer.forEach {
                    var bitrate = 0
                    it.bitrates.forEach { br->
                        if (br.isSelected) bitrate = it.bitrates.indexOf(br)
                    }
                    items.add(
                        MediaItem.Builder()
                            .setUri(Uri.parse(it.bitrates[bitrate].url))
                            .build()
                    )
                }
                player.clearMediaItems()
                player.setMediaItems(items)
                player.seekTo(AppData.stationsPlayer.indexOf(station), C.TIME_UNSET)
                player.prepare()
                handler.postDelayed({
                    if (player.playbackState != ExoPlayer.STATE_READY) {
                        handlePlayerError()
                    }
                }, 5000)
            }
        }

        setPlayer()
    }

    fun changeStation(station: Station, position: Int) {
        this.station = station
        bitrateIndex = 0
        AppData.stationsPlayer[position].bitrates.forEach { br ->
            if (br.isSelected) {
                bitrateIndex = station.bitrates.indexOf(br)
            }
        }
        player.seekTo(position, C.TIME_UNSET)
    }

    private fun setPlayer() {
        bitrateIndex = 0
        station.bitrates.forEach { br ->
            if (br.isSelected) {
                bitrateIndex = station.bitrates.indexOf(br)
            }
        }
        player.addMetadataOutput { metadata ->
            for (n in 0 until metadata.length()) {
                when (val md = metadata[n]) {
                    is com.google.android.exoplayer2.metadata.icy.IcyInfo -> {
                        val track = md.title?:""
                        station.track = Html.fromHtml(track).toString()
                        if (!fromAlarm)
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
                        if (!fromAlarm)
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_stop_record"))
                    }
                } else {
                    handler.removeCallbacksAndMessages(null)
                    station.bitrates.forEach { bitrate -> bitrate.isSelected = false }
                    if (bitrateIndex>=station.bitrates.size) bitrateIndex = 0
                    station.bitrates[bitrateIndex].isSelected = true
                    stopped = false
                    player.setHandleAudioBecomingNoisy(
                        AppData
                            .getSettingBoolean(this@PlayerService, "headphone")
                    )
                }
//                if (!fromAlarm)
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_state_changed").apply {
                        putExtra("isPlaying", isPlaying)
                    })
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                if (!fromAlarm&&AppData.stations.size>0)
                station = if (events[events.size()-1]==4) Station()
                else AppData.stationsPlayer[player.currentWindowIndex]
            }

            override fun onPlayerError(error: PlaybackException) {
                //handlePlayerError()
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
//                    val serviceBundle = Bundle()
//                    serviceBundle.putSerializable("station", station)
//                    intent1.putExtra("bundle", serviceBundle)
                    return PendingIntent.getActivity(
                        applicationContext, 0,
                        intent1, 0)
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
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_close"))
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_stop_record"))
                    exitService()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean) {
                    if (!stopped) startForeground(notificationId, notification)
                }
            })
            .build()
        playerNotificationManager?.apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_radio)
            setUseStopAction(true)
            setPriority(NotificationCompat.PRIORITY_MAX)
            setPlayer(player)
            setUseNextAction(true)
            setUsePreviousAction(true)
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
//            player.setMediaItem(
//                MediaItem.Builder()
//                    .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
//                    .build()
//            )
//            player.prepare()
            if (AppData.getSettingInt(this, "timer") > 0) {
                setTimerOff()
            }
        } else stopSelf()
    }

    private fun setVolume() {
        defaultVolume = audioManager.getStreamVolume(STREAM_MUSIC)
        audioManager.setStreamVolume(STREAM_MUSIC, 6,
            FLAG_PLAY_SOUND)
        if (AppData.getSettingBoolean(this,"volume")){
            player.volume = 0f
            val countDownTimer = object: CountDownTimer(60000,2000){
                override fun onTick(millisUntilFinished: Long) {
                    player.volume += 0.02f
                }
                override fun onFinish() {
                }
            }
            countDownTimer.start()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    fun setTimerOff() {
        val timer = Timer()
        val hourlyTask: TimerTask = object : TimerTask() {
            override fun run() {
                CoroutineScope(Dispatchers.Main).launch {
                    AppData.setSettingInt(this@PlayerService, "timer",0)
                    stopped = true
                    playerNotificationManager?.setPlayer(null)
                    station = Station()
                    if (!fromAlarm) {
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_close"))
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_stop_record"))
                        player.stop()
                    }
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        timer.schedule (hourlyTask, AppData.getSettingInt(this,"timer")*60*1000L)
    }


    private fun handlePlayerError() {
        Log.v("DASD","player error: ${station.name}, bitrate $bitrateIndex")
        handler.removeCallbacksAndMessages(null)
        if (isInternetAvailable()) {
            bitrateIndex++
            if (bitrateIndex < station.bitrates.size) {
                setBitrate(bitrateIndex)
            } else {
                if (fromAlarm) {
                    exitService()
                }
                LocalBroadcastManager.getInstance(this@PlayerService)
                    .sendBroadcast(Intent("player_state_changed").apply {
                        putExtra("isPlaying", false)
                        putExtra("isError", true)
                    })
            }
        } else {
            if (AppData.getSettingBoolean(this@PlayerService,"reconnect")
                ||fromAlarm) {
                applicationContext.registerReceiver(mConnReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            } else {
                if (!fromAlarm) {
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_close"))
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("no_internet"))
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_stop_record"))
                }
                exitService()
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

    private fun startTimerRecord(){
        val startTime = System.currentTimeMillis()
        val r: Runnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, 1000)
                val time = SimpleDateFormat("mm:ss", Locale.getDefault())
                    .format(System.currentTimeMillis()-startTime)
                if (!fromAlarm)
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
    }

    fun setBitrate(index: Int){
        bitrateIndex = index
        val position = AppData.stationsPlayer.indexOf(station)
        player.removeMediaItem(position)
        player.addMediaItem(position,
            MediaItem.Builder()
                .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                .build())
        player.seekTo(position,0)
        player.prepare()
        handler.postDelayed({
            if (player.playbackState != ExoPlayer.STATE_READY) {
                handlePlayerError()
            }
        }, 5000)
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

    fun exitService(){
        player.stop()
        stopped = true
        station = Station()
        playerNotificationManager?.setPlayer(null)
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        if (defaultVolume>-1) {
            audioManager.setStreamVolume(
                STREAM_MUSIC, defaultVolume,
                FLAG_PLAY_SOUND)
        }
        handler.removeCallbacksAndMessages(null)
        AppData.stationsPlayer.clear()
        station = Station()
        alarm = Alarm()
        fromAlarm = false
        super.onDestroy()
    }
}