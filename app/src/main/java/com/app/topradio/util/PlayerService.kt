package com.app.topradio.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.topradio.R
import com.app.topradio.model.Station
import com.app.topradio.ui.MainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerNotificationManager


class PlayerService: Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    var station = Station()
    var stopped = false
    var bitrateIndex = 0

    override fun onBind(intent: Intent?): IBinder {
        return PlayerServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        player = SimpleExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra("bundle")?.let{
            station = it.getSerializable("station") as Station
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
                    LocalBroadcastManager.getInstance(this@PlayerService)
                        .sendBroadcast(Intent("player_state_changed").apply {
                            putExtra("isPlaying", isPlaying)
                        })
                    if (!isPlaying) {
                        stopped = true
                        stopForeground(false)
                    } else stopped = false
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    bitrateIndex++
                    if (bitrateIndex<station.bitrates.size){
                        player.setMediaItem(
                            MediaItem.Builder()
                                .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                                .build())
                        player.prepare()
                    } else {
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_state_changed").apply {
                                putExtra("isPlaying", false)
                            })
                    }
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
                            Intent(applicationContext, MainActivity::class.java), 0
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
                        LocalBroadcastManager.getInstance(this@PlayerService)
                            .sendBroadcast(Intent("player_close"))
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
            }
            player.playWhenReady = true
            if (bitrateIndex>=station.bitrates.size) bitrateIndex = 0
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.parse(station.bitrates[bitrateIndex].url))
                    .build())
            player.prepare()
        }

        return START_STICKY
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

    inner class PlayerServiceBinder : Binder() {
        val service
            get() = this@PlayerService

        val player
            get() = this@PlayerService.player

        val station get() = this@PlayerService.station
    }
}