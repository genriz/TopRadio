package com.app.topradio.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.app.topradio.util.AppData
import com.app.topradio.R
import com.app.topradio.databinding.ActivityMainBinding
import com.app.topradio.model.MainViewModel
import com.app.topradio.util.PlayerService
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetBehavior


class MainActivity : AppCompatActivity() {

    val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var service: PlayerService
    private lateinit var player: SimpleExoPlayer
    private var bound = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            if (service.station.name!="") {
                bound = true
                viewModel.station.value = binder.station
                showPlayer()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bound = false
        }
    }

    private val playerStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "player_state_changed") {
                viewModel.station.value!!.isPlaying =
                    intent.getBooleanExtra("isPlaying", false)
                viewModel.station.value = viewModel.station.value
                if (!viewModel.station.value!!.isPlaying){
                    if (player.playbackState==ExoPlayer.STATE_BUFFERING){
                        binding.progressPLayer.visibility = View.VISIBLE
                    } else binding.progressPLayer.visibility = View.GONE
                } else binding.progressPLayer.visibility = View.GONE
            }
            if (intent?.action == "player_track_name") {
                viewModel.station.value!!.track = intent.getStringExtra("track_name")?:""
                viewModel.station.value = viewModel.station.value
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_state_changed"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_track_name"))

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)

        BottomSheetBehavior.from(binding.playerView).skipCollapsed = true
        BottomSheetBehavior.from(binding.playerView).state = BottomSheetBehavior.STATE_HIDDEN

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.menu_home,
                R.id.menu_genres,
                R.id.menu_cities,
                R.id.menu_viewed
            ),
            findViewById(R.id.drawerLayout)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        setupWithNavController(binding.navView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id){
                R.id.menu_home -> {
                    supportActionBar!!.title = ""
                    supportActionBar!!.setIcon(R.drawable.logo_toolbar)
                }
                R.id.favorites -> {
                    supportActionBar!!.title = getString(R.string.favorites)
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_genres -> {
                    supportActionBar!!.title = getString(R.string.menu_genres)
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_cities -> {
                    supportActionBar!!.title = getString(R.string.menu_cities)
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_viewed -> {
                    supportActionBar!!.title = getString(R.string.menu_viewed)
                    supportActionBar!!.setIcon(null)
                }
            }
        }

        viewModel.stations.observe(this,{
            if (it!=null){
                viewModel.stations.removeObservers(this)
                bindService(Intent(this, PlayerService::class.java),
                    serviceConnection, Context.BIND_AUTO_CREATE)
            }
        })

        viewModel.favorites = AppData.getFavorites(this)
        viewModel.getStations()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.app_name),
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(Uri.parse(""), AudioAttributes.Builder().build())
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPlayer(){
        binding.progressPLayer.visibility = View.VISIBLE
        BottomSheetBehavior.from(binding.playerView).state = BottomSheetBehavior.STATE_EXPANDED
        binding.playPause.setOnClickListener {
            viewModel.station.value!!.isPlaying = !viewModel.station.value!!.isPlaying
            viewModel.station.value = viewModel.station.value!!
            if (!viewModel.station.value!!.isPlaying) player.pause()
            else player.play()
        }
        binding.playerView.setOnTouchListener { _, _ ->
            true
        }

        if (!bound){
            val intent = Intent(this, PlayerService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("station", viewModel.station.value!!)
            intent.putExtra("bundle", serviceBundle)
            startService(intent)
        } else {
            binding.progressPLayer.visibility = View.GONE
            bound = false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navigateUp(navController, appBarConfiguration)
    }

    override fun onResume() {
        super.onResume()
        binding.trackName.isSelected = true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }

}