package ru.topradio.ui

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
import ru.topradio.R
import ru.topradio.databinding.ActivityMainBinding
import ru.topradio.model.MainViewModel
import ru.topradio.model.State
import ru.topradio.model.Station
import ru.topradio.ui.adapters.OnClick
import ru.topradio.ui.adapters.PlayerPagerAdapter
import ru.topradio.ui.dialogs.DialogInternet
import ru.topradio.ui.dialogs.DialogMenu
import ru.topradio.ui.dialogs.DialogRecord
import ru.topradio.util.AppData
import ru.topradio.util.PlayerService
import com.google.android.exoplayer2.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import pub.devrel.easypermissions.EasyPermissions
import ru.topradio.ui.dialogs.DialogStationOff
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
    private val playerStationsAdapter by lazy {PlayerPagerAdapter(this, this,
        viewModel.showAds,this)}
    private val android11StorageRequest = 3434
    private var selectedBitrate = 0
    private var stationBitrate = 0
    private val dialogMenu by lazy { DialogMenu(this, this) }
    var scrollToFirst = false
    private val handlerAds = Handler(Looper.getMainLooper())

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            if (binder.station.name!="") {
                bound = true
                viewModel.station.value = AppData.getStationById(binder.station.id)
                viewModel.station.value!!.isPlaying = binder.station.isPlaying
                viewModel.station.value!!.track = binder.station.track
                viewModel.station.value!!.bitrates = binder.station.bitrates
                viewModel.playerRecording.value = binder.station.isRecording
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
                viewModel.station.value = viewModel.getStationById(service.station, AppData.stations)
                viewModel.station.value!!.isPlaying =
                    intent.getBooleanExtra("isPlaying", false)
                viewModel.station.value!!.bitrates = service.station.bitrates
                service.station.bitrates.forEach {
                    if (it.isSelected) stationBitrate = service.station.bitrates.indexOf(it)
                }
                if (!viewModel.station.value!!.isPlaying){
                    if (player.playbackState==ExoPlayer.STATE_BUFFERING){
                        viewModel.playerWaiting.value =
                            viewModel.station.value!!.id==viewModel.stationPager.value!!.id
                    } else {
                        if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id)
                            viewModel.stationPager.value = viewModel.station.value
                        viewModel.playerWaiting.value = false
                    }
                    if (intent.getBooleanExtra("isError", false)){
                        showStationOffDialog()
                        viewModel.playerWaiting.value = false
                        service.station = Station()
                    }
                } else {
                    if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id) {
                        viewModel.stationPager.value = viewModel.station.value
                    } else {
                        viewModel.stationPager.value!!.isPlaying = false
                        viewModel.stationPager.value = viewModel.stationPager.value
                        val position = viewModel.getStationPosition(viewModel.station.value!!,
                            AppData.stationsPlayer)
                        binding.playerView.playerPager
                            .setCurrentItem(position, false)
                        playerStationsAdapter.notifyItemChanged(position)
                    }
                    playerStationsAdapter
                        .notifyItemChanged(viewModel
                            .getStationPosition(viewModel.stationPager.value!!,
                                AppData.stationsPlayer))
                    viewModel.playerWaiting.value = false
                }
            }
            if (intent?.action == "player_track_name") {
                viewModel.station.value!!.track = intent.getStringExtra("track_name")?:""
                viewModel.station.value = viewModel.station.value
                if (viewModel.stationPager.value!!.id==viewModel.station.value!!.id)
                    viewModel.stationPager.value = viewModel.station.value!!
                playerStationsAdapter
                    .notifyItemChanged(viewModel.getStationPosition(viewModel.station.value!!,
                        AppData.stationsPlayer))
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
            if (intent?.action == "no_internet") {
                showInternetDialog()
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
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playerStateChangedReceiver, IntentFilter("no_internet"))

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
                //R.id.menu_viewed,
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
                    supportActionBar!!.title = viewModel.stationPager.value!!.name
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

        MobileAds.initialize(this) {}

        if (AppData.getSettingBoolean(this,"show_favorites")){
            navController.navigate(R.id.favorites)
        }

        viewModel.state.observe(this,{
            if (it!=null&&it==State.STATE_FAILED) showInternetDialog()
        })

        //TODO
        viewModel.showAds.observe(this,{
            it?.let{show->
                handlerAds.removeCallbacksAndMessages(null)
                if (!show){
                    handlerAds.postDelayed({
                        viewModel.showAds.postValue(true)
                    },120000)
                }
            }
        })

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
                if (AppData.getSettingBoolean(this@MainActivity,"autoplay")
                    &&service.station.id!=viewModel.stationPager.value!!.id) {
                    playStation(viewModel.stationPager.value!!)
                } else {
                    if (viewModel.stationPager.value!!.id!=service.station.id){
                        viewModel.playerWaiting.value = false
                    }
                }
                viewModel.setViewedStation(this@MainActivity,
                    playerStationsAdapter.currentList[position])
            }
        })
        binding.playerView.playPause.setOnClickListener {
            if (service.station.id==viewModel.station.value!!.id){
                if (viewModel.station.value!!.isPlaying)
                    player.pause()
                else {
                    //viewModel.playerWaiting.value = true
                    player.prepare()
                    player.play()
                }
            } else {
                playStation(viewModel.station.value!!)
            }
        }
        binding.playerView.playPauseExtended.setOnClickListener {
            if (service.station.id == viewModel.stationPager.value!!.id
                //&&stationBitrate==selectedBitrate
            ) {
                if (viewModel.station.value!!.isPlaying)
                    player.pause()
                else {
                    //viewModel.playerWaiting.value = true
                    player.prepare()
                    player.play()
                }
            } else
                playStation(viewModel.stationPager.value!!)
        }
        binding.playerView.favoritePlayer.setOnClickListener {
            viewModel.station.value!!.isFavorite = !viewModel.station.value!!.isFavorite
            viewModel.updateItemPosition.value = viewModel
                .getStationPosition(viewModel.station.value!!,viewModel.stations.value!!)
            viewModel.updateStationFavorite(this, viewModel.station.value!!)
        }
        binding.playerView.favoritePlayerExpanded.setOnClickListener {
            viewModel.stationPager.value!!.isFavorite = !viewModel.stationPager.value!!.isFavorite
            viewModel.stationPager.value = viewModel.stationPager.value
            viewModel.updateItemPosition.value = viewModel
                .getStationPosition(viewModel.stationPager.value!!,viewModel.stations.value!!)
            viewModel.updateStationFavorite(this, viewModel.stationPager.value!!)
            playerStationsAdapter.notifyItemChanged(viewModel
                .getStationPosition(viewModel.stationPager.value!!,viewModel.stations.value!!))
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
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED){
                        if (service.station.name==""){
                            BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                            BottomSheetBehavior.from(binding.playerView.root).state =
                                BottomSheetBehavior.STATE_HIDDEN
                        } else {
                            playerStationsAdapter.submitList(AppData.stationsPlayer){
                                val position = viewModel.getStationPosition(viewModel.station.value!!,
                                    AppData.stationsPlayer)
                                binding.playerView.playerPager
                                    .setCurrentItem(position, false)
                                playerStationsAdapter.notifyItemChanged(position)
                            }
                            BottomSheetBehavior.from(binding.playerView.root).isHideable = false
                        }
                        binding.playerView.playerExpanded.visibility = View.GONE
                        hideKeyboard()
                    } else
                    if (newState == BottomSheetBehavior.STATE_EXPANDED){
                        if (playerStationsAdapter.currentList.size==1){
                            binding.playerView.playerPager.postDelayed({
                                playerStationsAdapter.notifyItemChanged(0)
                            },100)
                        }
                        binding.playerView.playerMini.visibility = View.GONE
                        hideKeyboard()
                        if (AppData.getSettingBoolean(this@MainActivity,"autoplay")
                            &&service.station.id!=viewModel.stationPager.value!!.id) {
                            playStation(viewModel.stationPager.value!!)
                        }
                    } else
                    if (newState == BottomSheetBehavior.STATE_DRAGGING){
                        if (!binding.playerView.playerExpanded.isVisible) {
                            binding.playerView.playerExpanded.visibility = View.VISIBLE
                        }
                        if (!binding.playerView.playerMini.isVisible) {
                            binding.playerView.playerMini.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.playerView.playerExpanded.alpha = slideOffset
                    binding.playerView.playerMini.alpha = 1 - slideOffset
                    binding.navHostFragment.setPadding(0, 0, 0,
                        (binding.navHostFragment.bottom-bottomSheet.top))
                }

            })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showPlayer(mainStations: Boolean){
        val position = if (mainStations)
            viewModel.getStationPosition(viewModel.stationPager.value!!,viewModel.stations.value!!)
        else viewModel.getStationFavoritesPosition(viewModel.stationPager.value!!)
        binding.playerView.playerExpanded.visibility = View.VISIBLE
        if (!mainStations)
            playerStationsAdapter.submitList(viewModel.stationsFavorites.value!!){
                binding.playerView.playerPager
                    .setCurrentItem(position, false)
                playerStationsAdapter.notifyItemChanged(position)
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        else
            playerStationsAdapter.submitList(viewModel.stations.value!!){
                binding.playerView.playerPager
                    .setCurrentItem(position, false)
                playerStationsAdapter.notifyItemChanged(position)
                BottomSheetBehavior.from(binding.playerView.root).state =
                    BottomSheetBehavior.STATE_EXPANDED
            }


        //TODO
        //viewModel.showAds.value = true
    }

    private fun playStation(station: Station){
        viewModel.playerWaiting.value = true
        if (!bound){
            viewModel.stations.value!!.forEach {
                it.isPlaying = false
                it.track = ""
            }
            playerStationsAdapter.notifyItemChanged(viewModel
                .getStationPosition(viewModel.station.value!!,viewModel.stations.value!!))
            viewModel.station.value = viewModel.stationPager.value
            AppData.stationsPlayer.clear()
            AppData.stationsPlayer.addAll(viewModel.stations.value!!)
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
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            EasyPermissions.requestPermissions(
                this,
                "Top Radio needs permission.\nPress OK to continue.",
                282,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R&&!Environment.isExternalStorageManager()){
                val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
                intent.data = Uri.parse("package:ru.topradio")
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
            intent.data = Uri.parse("package:ru.topradio")
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
        selectedBitrate = position
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

    private fun showInternetDialog(){
        DialogInternet(this).show()
    }

    private fun showStationOffDialog(){
        DialogStationOff(this).show()
    }

    override fun onMenuPositionClick(position: Int) {
        when (position){
            0 -> navController.navigate(R.id.menu_alarm)
            1 -> openAddStation()
            2 -> openFeedback()
            3 -> navController.navigate(R.id.menu_settings)
            4 -> {
                service.exitService()
                finish()
            }
        }
    }

    private fun openFeedback() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://play.google.com/store/apps/details?id=ru.topradio")
        startActivity(Intent.createChooser(intent, "Complete action using"))
    }

    private fun openAddStation() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://top-radio.ru/dobavit-radio")
        startActivity(Intent.createChooser(intent, "Complete action using"))
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        if (navController.currentDestination?.id==R.id.favorites)
            AppData.setSettingBoolean(this,"show_favorites",true)
        else
            AppData.setSettingBoolean(this,"show_favorites",false)
        handlerAds.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

}