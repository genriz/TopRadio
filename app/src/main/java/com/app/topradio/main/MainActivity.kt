package com.app.topradio.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.app.topradio.R
import com.app.topradio.databinding.ActivityMainBinding
import com.app.topradio.model.Station
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity() {

    val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchView: SearchView
    private lateinit var service: PlayerService
    private lateinit var player: SimpleExoPlayer
    private var bound = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            bound = true
            showPlayer(binder.station)
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
        binding.toolbar.visibility = View.GONE

        BottomSheetBehavior.from(binding.playerView).skipCollapsed = true
        BottomSheetBehavior.from(binding.playerView).state = BottomSheetBehavior.STATE_HIDDEN

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.menu_home
            ),
            findViewById(R.id.drawerLayout)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController(findViewById<NavigationView>(R.id.nav_view),
            navController)

        viewModel.stations.observe(this,{
            if (it!=null&&binding.toolbar.visibility==View.GONE){
                navController.navigate(R.id.toHome)
                binding.toolbar.visibility = View.VISIBLE
            }
        })

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

    fun showPlayer(station: Station){
        if (bound){
            viewModel.station.value = station
            BottomSheetBehavior.from(binding.playerView).state = BottomSheetBehavior.STATE_EXPANDED
            binding.playPause.setOnClickListener {
                station.isPlaying = !station.isPlaying
                viewModel.station.value = station
                if (!station.isPlaying) player.pause()
                else player.play()
            }
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.parse(station.bitrates[0].url))
                    .build())
            player.prepare()
            service.station = station
        } else {
            val intent = Intent(this, PlayerService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("station", station)
            intent.putExtra("bundle", serviceBundle)
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.apply {
            maxWidth = Integer.MAX_VALUE
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object: SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }
                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.length>2)
                        viewModel.searchStations(newText)
                    else if (newText.isEmpty()) viewModel.clearSearch()
                    return true
                }

            })
            setOnCloseListener {
                viewModel.clearSearch()
                onActionViewCollapsed()
                true
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        binding.trackName.isSelected = true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else {
            if (!searchView.isIconified) {
                searchView.isIconified = true
            } else {
                super.onBackPressed();
            }
        }
    }

}