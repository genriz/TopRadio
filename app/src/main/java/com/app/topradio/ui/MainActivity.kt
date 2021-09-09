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
import android.os.*
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.topradio.R
import com.app.topradio.databinding.ActivityMainBinding
import com.app.topradio.model.Bitrate
import com.app.topradio.model.MainViewModel
import com.app.topradio.model.Station
import com.app.topradio.ui.adapters.OnClick
import com.app.topradio.ui.adapters.PlayerPagerAdapter
import com.app.topradio.util.AppData
import com.app.topradio.util.PlayerService
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, OnClick,
    DialogRecord.OnClick, DialogMenu.OnDialogMenuClick {

    val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var service: PlayerService
    lateinit var player: SimpleExoPlayer
    private var bound = false
    private val playerStationsAdapter = PlayerPagerAdapter(this, this)
    private val android11StorageRequest = 3434
    private val dialogMenu by lazy { DialogMenu(this, this) }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            if (binder.station.name!="") {
                bound = true
                viewModel.station.value = binder.station
                viewModel.stationPager.value = viewModel.station.value!!
                showPlayer(true)
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
                viewModel.station.value!!.bitrates = service.station.bitrates
                viewModel.station.value = viewModel.station.value
                if (!viewModel.station.value!!.isPlaying){
                    if (player.playbackState==ExoPlayer.STATE_BUFFERING){
                        viewModel.playerWaiting.value = true
                    } else {
                        if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id)
                            viewModel.stationPager.value = viewModel.station.value
                        viewModel.playerWaiting.value = false
                    }
                } else {
                    if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id) {
                        viewModel.stationPager.value = viewModel.station.value
                        playerStationsAdapter
                            .notifyItemChanged(viewModel.getStationPosition(viewModel.stationPager.value!!))
                    }
                    viewModel.playerWaiting.value = false
                }
            }
            if (intent?.action == "player_track_name") {
                viewModel.station.value!!.track = intent.getStringExtra("track_name")?:""
                viewModel.station.value = viewModel.station.value
                if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id)
                    viewModel.stationPager.value = viewModel.station.value!!
                playerStationsAdapter
                    .notifyItemChanged(viewModel.getStationPosition(viewModel.station.value!!))
            }
            if (intent?.action == "player_close") {
                BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_HIDDEN
                viewModel.playerWaiting.value = false
            }
            if (intent?.action == "player_stop_record") {
                stopRecord()
            }
            if (intent?.action == "player_record_time") {
                viewModel.recordTime.postValue(intent.getStringExtra("time")?:"")
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
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_stop_record"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("player_record_time"))

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
                R.id.menu_viewed,
                R.id.menu_records
            ),
            findViewById(R.id.drawerLayout)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        setupWithNavController(binding.navView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (BottomSheetBehavior.from(binding.playerView.root).state
                == BottomSheetBehavior.STATE_EXPANDED
                &&destination.id!=R.id.menu_playlist) {
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
                R.id.menu_records -> {
                    supportActionBar!!.title = getString(R.string.menu_records)
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_settings -> {
                    supportActionBar!!.title = getString(R.string.menu_settings)
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_playlist -> {
                    BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                    BottomSheetBehavior.from(binding.playerView.root).state =
                        BottomSheetBehavior.STATE_HIDDEN
                    supportActionBar!!.title = viewModel.station.value!!.name
                    supportActionBar!!.setIcon(null)
                }
                R.id.menu_alarm -> {
                    supportActionBar!!.title = getString(R.string.menu_alarm)
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

        setupBottomSheet()

    }

    private fun setupBottomSheet() {
        binding.playerView.playerPager.adapter = playerStationsAdapter
        val recycler = binding.playerView.playerPager.getRecycler()
        recycler?.let{view ->
            view.isNestedScrollingEnabled = false
        }
        binding.playerView.playerPager.registerOnPageChangeCallback(object:ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.stationPager.value = playerStationsAdapter.currentList[position]
            }
        })
        binding.playerView.playPause.setOnClickListener {
            if (service.station.id==viewModel.station.value!!.id){
                if (viewModel.station.value!!.isPlaying)
                    player.pause()
                else {
                    viewModel.playerWaiting.value = true
                    player.prepare()
                    player.play()
                }
            } else {
                playStation(viewModel.station.value!!)
            }
        }
        binding.playerView.playPauseExtended.setOnClickListener {
            if (service.station.id == viewModel.stationPager.value!!.id) {
                if (viewModel.station.value!!.isPlaying)
                    player.pause()
                else {
                    viewModel.playerWaiting.value = true
                    player.prepare()
                    player.play()
                }
            } else
                playStation(viewModel.stationPager.value!!)
        }
        binding.playerView.favoritePlayer.setOnClickListener {
            viewModel.station.value!!.isFavorite = !viewModel.station.value!!.isFavorite
            viewModel.updateItemPosition.value = viewModel.getStationPosition(viewModel.station.value!!)
            viewModel.updateStationFavorite(this, viewModel.station.value!!)
        }
        binding.playerView.favoritePlayerExpanded.setOnClickListener {
            viewModel.stationPager.value!!.isFavorite = !viewModel.stationPager.value!!.isFavorite
            viewModel.stationPager.value = viewModel.stationPager.value
            viewModel.updateItemPosition.value = viewModel.getStationPosition(viewModel.stationPager.value!!)
            viewModel.updateStationFavorite(this, viewModel.stationPager.value!!)
            playerStationsAdapter.notifyItemChanged(viewModel.getStationPosition(viewModel.stationPager.value!!))
        }
        binding.playerView.recordExtended.setOnClickListener {
            if (player.isPlaying) {
                if (!viewModel.playerRecording.value!!)
                    checkPermissions()
                else {
                    stopRecord()
                }
            }
        }
        binding.playerView.playlistExtended.setOnClickListener {
            navController.navigate(R.id.menu_playlist)
        }
        binding.playerView.root.setOnClickListener {
            binding.playerView.playerExpanded.visibility = View.VISIBLE
            BottomSheetBehavior.from(binding.playerView.root).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

        BottomSheetBehavior.from(binding.playerView.root)
            .addBottomSheetCallback(object:BottomSheetBehavior.BottomSheetCallback(){
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState){
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            if (viewModel.station.value!!.id
                                !=viewModel.stationPager.value!!.id){
                                viewModel.stationPager.value = viewModel.station.value
                                if (viewModel.stations.value!!.contains(
                                        viewModel.getStationById(viewModel.station.value!!))){
                                    viewModel.stationPager.value = viewModel.station.value
                                    val position = viewModel.getStationPosition(
                                        viewModel.stationPager.value!!)
                                    binding.playerView.playerPager.postDelayed({
                                        binding.playerView.playerPager
                                            .setCurrentItem(position, false)
                                        playerStationsAdapter.notifyItemChanged(position)
                                    },100)
                                } else {
                                    updatePlayerPager()
                                }

                            }
                            if (service.station.name==""){
                                BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                                BottomSheetBehavior.from(binding.playerView.root).state =
                                    BottomSheetBehavior.STATE_HIDDEN
                            } else BottomSheetBehavior.from(binding.playerView.root).isHideable = false
                            binding.playerView.playerExpanded.visibility = View.GONE
                            hideKeyboard()
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            if (playerStationsAdapter.currentList.size==1){
                                binding.playerView.playerPager.postDelayed({
                                    playerStationsAdapter.notifyItemChanged(0)
                                },100)
                            }
                            binding.playerView.playerMini.visibility = View.GONE
                            hideKeyboard()
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            if (!binding.playerView.playerExpanded.isVisible) {
                                binding.playerView.playerExpanded.visibility = View.VISIBLE
                            }
                            if (!binding.playerView.playerMini.isVisible) {
                                binding.playerView.playerMini.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.playerView.playerExpanded.alpha = slideOffset
                    binding.playerView.playerMini.alpha = 1 - slideOffset
                    val currentHeight = binding.navHostFragment.height - bottomSheet.height
                    val bottomSheetShiftDown = currentHeight - bottomSheet.top
                    binding.navHostFragment.setPadding(0, 0, 0,
                        (bottomSheet.height + bottomSheetShiftDown))
                }

            })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPlayer(withPager: Boolean){
        val position = viewModel.getStationPosition(viewModel.stationPager.value!!)
        binding.playerView.playerExpanded.visibility = View.VISIBLE
        playerStationsAdapter.submitList(viewModel.stations.value!!)
        binding.playerView.playerPager.postDelayed({
            binding.playerView.playerPager
                .setCurrentItem(position, false)
            playerStationsAdapter.notifyItemChanged(position)
        },100)
        if (AppData.getSettingBoolean(this,"autoplay")) {
            playStation(viewModel.stationPager.value!!)
        }
        BottomSheetBehavior.from(binding.playerView.root).state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun playStation(station: Station){
        viewModel.playerWaiting.value = true
        if (!bound){
            viewModel.stations.value!!.forEach {
                it.isPlaying = false
                it.track = ""
            }
            playerStationsAdapter.notifyItemChanged(viewModel.getStationPosition(viewModel.station.value!!))
            viewModel.station.value = viewModel.stationPager.value
            val intent = Intent(this, PlayerService::class.java)
            val serviceBundle = Bundle()
            serviceBundle.putSerializable("station", station)
            serviceBundle.putBoolean("fromAlarm", false)
            intent.putExtra("bundle", serviceBundle)
            startService(intent)
            stopRecord()
        } else {
            viewModel.playerWaiting.postValue(false)
            bound = false
        }
    }

    private fun ViewPager2.getRecycler(): RecyclerView?{
        try{
            val field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            return field.get(this) as RecyclerView
        }catch (e:Exception){e.printStackTrace()}
        return null
    }

    fun updatePlayerPager(){
        viewModel.stationPager.value = viewModel.station.value
        playerStationsAdapter.submitList(ArrayList<Station>()
            .apply { add (viewModel.stationPager.value!!) })
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
        service.recordAudio()
    }

    private fun stopRecord(){
        if (viewModel.playerRecording.value!!){
            viewModel.playerRecording.postValue(false)
            viewModel.recordTime.postValue("")
            service.stopRecord()
            DialogRecord(this,this).show()
        }
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
        else {
            if (BottomSheetBehavior.from(binding.playerView.root).state
                == BottomSheetBehavior.STATE_EXPANDED) {
                binding.playerView.playerMini.visibility = View.VISIBLE
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_COLLAPSED
            } else if (dialogMenu.isShowing) dialogMenu.dismiss()
            else super.onBackPressed()
        }
    }

    fun toMiniPlayer(view: View) {
        binding.playerView.playerMini.visibility = View.VISIBLE
        BottomSheetBehavior.from(binding.playerView.root).state =
            BottomSheetBehavior.STATE_COLLAPSED
    }

    fun toExpandedPlayer(){
        binding.playerView.playerExpanded.visibility = View.VISIBLE
        BottomSheetBehavior.from(binding.playerView.root).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCopyClick(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("topradio", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    override fun onBitrateClick(position: Int) {
        if (viewModel.stationPager.value!!.isPlaying) {
            service.setBitrate(position)
            viewModel.station.value!!.bitrates.forEach { it.isSelected = false }
        } else {
            val positionPager = playerStationsAdapter.currentList
                .indexOf(viewModel.stationPager.value!!)
            viewModel.stationPager.value!!.bitrates.forEach {
                it.isSelected = false
            }
            viewModel.stationPager.value!!.bitrates[position].isSelected = true
            playerStationsAdapter
                .notifyItemChanged(positionPager)
        }
    }

    override fun openFolder() {
        navController.navigate(R.id.menu_records)
    }

    fun showMenuDialog(){
        dialogMenu.show()
    }

    override fun onMenuPositionClick(position: Int) {
        when (position){
            0 -> navController.navigate(R.id.menu_alarm)
            3 -> navController.navigate(R.id.menu_settings)
            4 -> finish()
        }
    }

}