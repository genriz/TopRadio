package com.app.topradio.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.ClipData
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import androidx.viewpager2.widget.ViewPager2
import com.app.topradio.R
import com.app.topradio.databinding.ActivityMainBinding
import com.app.topradio.model.MainViewModel
import com.app.topradio.model.Station
import com.app.topradio.ui.adapters.OnClick
import com.app.topradio.ui.adapters.PlayerPagerAdapter
import com.app.topradio.util.AppData
import com.app.topradio.util.PlayerService
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, OnClick {

    val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var service: PlayerService
    private lateinit var player: SimpleExoPlayer
    private var bound = false
    private val playerStationsAdapter = PlayerPagerAdapter(this)
    private var currentPagePosition = 0
    private val android11StorageRequest = 3434

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            if (service.station.name!="") {
                bound = true
                viewModel.station.value = AppData.getStationById(binder.station.id)
                if (player.isPlaying) {
                    viewModel.station.value!!.isPlaying = true
                    viewModel.station.value!!.track = binder.station.track
                    viewModel.station.value = viewModel.station.value
                    viewModel.stationPager.value = viewModel.station.value!!
                    currentPagePosition = playerStationsAdapter.currentList
                        .indexOf(viewModel.stationPager.value!!)
                    playerStationsAdapter
                        .notifyItemChanged(currentPagePosition)
                }
                service.station = viewModel.station.value!!
                showPlayer(false)
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
                        viewModel.playerWaiting.postValue(true)
                    } else {
                        viewModel.playerWaiting.postValue(false)
                    }
                } else {
                    viewModel.playerWaiting.postValue(false)
                }
                viewModel.stationPager.value = viewModel.station.value!!
                currentPagePosition = playerStationsAdapter.currentList
                    .indexOf(viewModel.stationPager.value!!)
                playerStationsAdapter
                    .notifyItemChanged(currentPagePosition)
            }
            if (intent?.action == "player_track_name") {
                viewModel.station.value!!.track = intent.getStringExtra("track_name")?:""
                viewModel.station.value = viewModel.station.value
                viewModel.stationPager.value = viewModel.station.value!!
                currentPagePosition = playerStationsAdapter.currentList
                    .indexOf(viewModel.stationPager.value!!)
                playerStationsAdapter
                    .notifyItemChanged(currentPagePosition)
            }
            if (intent?.action == "player_close") {
                BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_state_changed"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_track_name"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_close"))

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.playerView.viewModel = viewModel
        binding.playerView.lifecycleOwner = this
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)

        BottomSheetBehavior.from(binding.playerView.root).state = BottomSheetBehavior.STATE_HIDDEN

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
            if (BottomSheetBehavior.from(binding.playerView.root).state
                == BottomSheetBehavior.STATE_EXPANDED) {
                binding.playerView.playerMini.visibility = View.VISIBLE
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_COLLAPSED
            }
            when (destination.id){
                R.id.menu_home -> {
                    supportActionBar!!.title = ""
                    supportActionBar!!.setIcon(R.drawable.ic_logo)
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

        viewModel.getAllStations()

        bindService(Intent(this, PlayerService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPlayer(withPager: Boolean){
        viewModel.playerWaiting.postValue(true)

        binding.playerView.playPause.setOnClickListener {
            viewModel.station.value!!.isPlaying = !viewModel.station.value!!.isPlaying
            viewModel.station.value = viewModel.station.value!!
            if (!viewModel.station.value!!.isPlaying) player.pause()
            else player.play()
        }
        binding.playerView.playPauseExtended.setOnClickListener {
            if (service.station.id==viewModel.stationPager.value!!.id){
                viewModel.stationPager.value!!.isPlaying = !viewModel.stationPager.value!!.isPlaying
                viewModel.stationPager.value = viewModel.stationPager.value!!
                if (!viewModel.stationPager.value!!.isPlaying) player.pause()
                else player.play()
            } else {
                playerStationsAdapter.currentList[currentPagePosition].isPlaying = false
                playerStationsAdapter.currentList[currentPagePosition].track = ""
                playerStationsAdapter
                    .notifyItemChanged(currentPagePosition)
                currentPagePosition = playerStationsAdapter.currentList
                    .indexOf(viewModel.stationPager.value!!)
                viewModel.station.value = viewModel.stationPager.value
                showPlayer(withPager)
            }

        }
        binding.playerView.favoritePlayer.setOnClickListener {
            viewModel.station.value!!.isFavorite = !viewModel.station.value!!.isFavorite
            val position = viewModel.stations.value!!.indexOf(viewModel.station.value)
            viewModel.updateItemPosition.value = position
            viewModel.updateStation(this, viewModel.station.value!!)
        }
        binding.playerView.favoritePlayerExpanded.setOnClickListener {
            viewModel.station.value!!.isFavorite = !viewModel.station.value!!.isFavorite
            viewModel.stationPager.value = viewModel.station.value
            val position = viewModel.stations.value!!.indexOf(viewModel.station.value)
            viewModel.updateItemPosition.value = position
            viewModel.updateStation(this, viewModel.station.value!!)
        }

        binding.playerView.recordExtended.setOnClickListener {
            if (player.isPlaying) {
                if (!viewModel.playerRecording.value!!)
                    checkPermissions()
                else {
                    viewModel.playerRecording.postValue(false)
                    service.stopRecord()
//                    fileOutputStream.close()
                }
            }
        }

        binding.playerView.root.setOnClickListener {
            binding.playerView.playerExpanded.visibility = View.VISIBLE
            BottomSheetBehavior.from(binding.playerView.root).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

        currentPagePosition = viewModel.stations.value!!.indexOf(viewModel.station.value!!)

        binding.playerView.playerPager.adapter = playerStationsAdapter
        if (withPager) {
            playerStationsAdapter.submitList(viewModel.stations.value!!)
            viewModel.stationPager.value = viewModel.stations.value!![currentPagePosition]
            binding.playerView.playerPager
                .setCurrentItem(currentPagePosition, false)
            binding.playerView.playerPager.registerOnPageChangeCallback(object:ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.stationPager.value = playerStationsAdapter.currentList[position]
                }
            })
        } else {
            viewModel.stationPager.value = viewModel.station.value
            val list = ArrayList<Station>().apply { add (viewModel.station.value!!) }
            playerStationsAdapter.submitList(list)
        }

        BottomSheetBehavior.from(binding.playerView.root).state = BottomSheetBehavior.STATE_EXPANDED

        if (!bound){
            val intent = Intent(this, PlayerService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("station", viewModel.station.value!!)
            intent.putExtra("bundle", serviceBundle)
            startService(intent)
        } else {
            viewModel.playerWaiting.postValue(false)
            bound = false
        }

        BottomSheetBehavior.from(binding.playerView.root)
            .addBottomSheetCallback(object:BottomSheetBehavior.BottomSheetCallback(){
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState){
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            binding.playerView.playerExpanded.visibility = View.GONE
                            binding.playerView.playerMini.visibility = View.GONE
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            BottomSheetBehavior.from(binding.playerView.root).isHideable = false
                            binding.playerView.playerExpanded.visibility = View.GONE
                            binding.playerView.playerMini.visibility = View.VISIBLE
                            hideKeyboard()
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.playerView.playerExpanded.visibility = View.VISIBLE
                            binding.playerView.playerMini.visibility = View.GONE
                            viewModel.stationPager.value = viewModel.station.value
                            binding.playerView.playerPager
                                    .setCurrentItem(viewModel.stations.value!!
                                        .indexOf(viewModel.station.value!!), false)

                            hideKeyboard()
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            binding.playerView.playerExpanded.visibility = View.VISIBLE
                            binding.playerView.playerMini.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.playerView.playerExpanded.alpha = slideOffset*1.3f
                    binding.playerView.playerMini.alpha = 1 - slideOffset*1.3f
                }

            })
    }

    fun updatePlayerPager(){
        val list = ArrayList<Station>().apply { add (viewModel.station.value!!) }
        playerStationsAdapter.submitList(list)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navigateUp(navController, appBarConfiguration)
    }

    fun hideKeyboard(){
        val imm: InputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun checkPermissions() {
        if (!EasyPermissions.hasPermissions(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE)){
            EasyPermissions.requestPermissions(
                this,
                "Movies Portal needs permission.\nPress OK to continue.",
                282,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R&&!Environment.isExternalStorageManager()){
                val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
                intent.data = Uri.parse("package:com.app.mdo")
                startActivityForResult(intent, android11StorageRequest)
            } else recordAudio()
        }
    }

    private fun recordAudio() {
        viewModel.playerRecording.postValue(true)
        service.recordAudio(viewModel.station.value!!.bitrates[0].url)
//        val url = URL(viewModel.station.value!!.bitrates[0].url)
//        val folder = File(Environment.getExternalStorageDirectory(), "TopRadio")
//        val fileAudio = File(folder, "${Calendar.getInstance().timeInMillis}.mp3")
//        if (!folder.exists()) folder.mkdirs()
//        val inputStream = url.openStream()
//        fileOutputStream = FileOutputStream(fileAudio)
//        var c: Int
//        while (inputStream.read().also { c = it } != -1) {
//            fileOutputStream.write(c)
//            c++
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R&&!Environment.isExternalStorageManager()){
            val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
            intent.data = Uri.parse("package:com.app.mdo")
            startActivityForResult(intent, android11StorageRequest)
        } else recordAudio()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==android11StorageRequest) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if (Environment.isExternalStorageManager()) {
                    recordAudio()
                } else checkPermissions()
            } else recordAudio()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.trackName.isSelected = true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        if (BottomSheetBehavior.from(binding.playerView.root).state
            == BottomSheetBehavior.STATE_EXPANDED) {
            binding.playerView.playerMini.visibility = View.VISIBLE
            BottomSheetBehavior.from(binding.playerView.root).state =
                BottomSheetBehavior.STATE_COLLAPSED
        }
        else super.onBackPressed()
    }

    fun toMiniPlayer(view: View) {
        binding.playerView.playerMini.visibility = View.VISIBLE
        BottomSheetBehavior.from(binding.playerView.root).state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onCopyClick(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("topradio", text))
        Snackbar.make(binding.playerView.playPauseExtended, R.string.copied, Snackbar.LENGTH_SHORT).show()
    }

}