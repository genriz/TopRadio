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
import androidx.annotation.UiThread
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
import com.bumptech.glide.Glide
import ru.topradio.R
import ru.topradio.databinding.ActivityMainBinding
import ru.topradio.model.MainViewModel
import ru.topradio.model.State
import ru.topradio.model.Station
import ru.topradio.ui.adapters.PlayerPagerAdapter
import ru.topradio.ui.dialogs.DialogInternet
import ru.topradio.ui.dialogs.DialogMenu
import ru.topradio.ui.dialogs.DialogRecord
import ru.topradio.util.AppData
import ru.topradio.util.PlayerService
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mobile.ads.common.MobileAds
import pub.devrel.easypermissions.EasyPermissions
import ru.topradio.ui.dialogs.DialogStationOff
import java.lang.Exception


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    DialogRecord.OnClick, DialogMenu.OnDialogMenuClick, PlayerPagerAdapter.OnClick {

    val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var service: PlayerService? = null
    lateinit var player: SimpleExoPlayer
    private val playerStationsAdapter by lazy {PlayerPagerAdapter(this, this,
        viewModel,this)}
    private var selectedBitrate = 0
    private var stationBitrate = 0
    private val dialogMenu by lazy { DialogMenu(this, this) }
    var scrollToFirst = false
    private val handlerAds = Handler(Looper.getMainLooper())
    private var fromUser = false
    private var fromService = false
    private var clickedPosition = 0
    private val pagerStations = ArrayList<Station>()

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: PlayerService.PlayerServiceBinder = iBinder as PlayerService.PlayerServiceBinder
            service = binder.service
            player = binder.player
            if (binder.station.name!="") {
                viewModel.station.value = AppData.getStationById(binder.station.id)
                viewModel.station.value!!.isPlaying = player.isPlaying
                viewModel.station.value!!.track = binder.station.track
                viewModel.station.value!!.bitrates = binder.station.bitrates
                viewModel.playerRecording.value = binder.station.isRecording
                viewModel.stationPager.value = viewModel.station.value!!
                fromService = true
                showPlayer(AppData.stationsPlayer)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {

        }
    }

    private val playerStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (service!=null) {
                if (intent?.action == "player_state_changed") {
                    if (viewModel.playerRecording.value!!) stopRecord()
                    viewModel.station.value =
                        viewModel.getStationById(service!!.station, AppData.stations)
                    viewModel.station.value!!.isPlaying =
                        intent.getBooleanExtra("isPlaying", false)
                    viewModel.station.value!!.bitrates = service!!.station.bitrates
                    service!!.station.bitrates.forEach {
                        if (it.isSelected) stationBitrate = service!!.station.bitrates.indexOf(it)
                    }
                    if (!viewModel.station.value!!.isPlaying) {
                        if (player.playbackState == ExoPlayer.STATE_BUFFERING) {
                            viewModel.playerWaiting.value =
                                viewModel.station.value!!.id == viewModel.stationPager.value!!.id
                        } else {
                            if (viewModel.stationPager.value!!.id == viewModel.station.value!!.id)
                                viewModel.stationPager.value = viewModel.station.value
                            viewModel.playerWaiting.value = false
                        }
                        if (intent.getBooleanExtra("isError", false)) {
                            showStationOffDialog()
                            viewModel.playerWaiting.value = false
                        }
                    } else {
                        if (viewModel.stationPager.value!!.id == viewModel.station.value!!.id) {
                            viewModel.stationPager.value = viewModel.station.value
                        }

                        if (BottomSheetBehavior.from(binding.playerView.root).state ==
                            BottomSheetBehavior.STATE_HIDDEN
                        ) {
                            viewModel.stationPager.value = viewModel.station.value
                            showPlayer(AppData.stationsPlayer)
                        } else {
                            viewModel.playerWaiting.value = false
                        }

//                        val position = viewModel.getStationPosition(
//                            viewModel.station.value!!,
//                            AppData.stationsPlayer)
                        //playerStationsAdapter.notifyItemChanged(position)
                    }
                }
                if (intent?.action == "player_track_name") {
                    val track = intent.getStringExtra("track_name") ?: ""
                    viewModel.station.value!!.track = track
                    viewModel.station.value = viewModel.station.value
                    if (viewModel.stationPager.value!!.id == viewModel.station.value!!.id)
                        viewModel.stationPager.value = viewModel.station.value!!

                    playerStationsAdapter.notifyItemChanged(
                        viewModel.getStationPosition(
                            viewModel.station.value!!,
                            AppData.stationsPlayer
                        ), track
                    )
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
                    viewModel.recordTime.postValue(intent.getStringExtra("time") ?: "")
                }
                if (intent?.action == "no_internet") {
                    showInternetDialog()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    supportActionBar!!.setIcon(R.drawable.ic_logo2)
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
//                    BottomSheetBehavior.from(binding.playerView.root).isHideable = true
//                    BottomSheetBehavior.from(binding.playerView.root).state =
//                        BottomSheetBehavior.STATE_HIDDEN
//                    supportActionBar!!.title = viewModel.stationPager.value!!.name
//                    supportActionBar!!.setIcon(null)
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

        //MobileAds.initialize(this) {}

        if (AppData.getSettingBoolean(this,"show_favorites")){
            navController.navigate(R.id.favorites)
        }

        viewModel.state.observe(this) {
            if (it != null && it == State.STATE_FAILED) showInternetDialog()
        }

        viewModel.loadAds.observe(this) {
            it?.let { loadAds ->
                handlerAds.removeCallbacksAndMessages(null)
                if (!loadAds) {
                    handlerAds.postDelayed({
                        viewModel.loadAds.postValue(true)
                    }, 120000)
                }
            }
        }

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
                if (service!=null) {
                    if (position != clickedPosition) {
                        viewModel.stationPager.value = pagerStations[position]
                        fromUser = true
                        clickedPosition = -1
                    }
                    if (AppData.getSettingBoolean(this@MainActivity, "autoplay")
                        && service!!.station.id != viewModel.stationPager.value!!.id
                        && fromUser) {
                            Log.v("DASD", "1")
                        viewModel.stationPager.value = pagerStations[position]
                        viewModel.playerWaiting.value = true
                        service!!.changeStation(viewModel.stationPager.value!!, position)
                    } else {
                        Log.v("2", "1")
                        if (viewModel.stationPager.value!!.id != service!!.station.id) {
                            viewModel.stationPager.value!!.isPlaying = false
                            viewModel.stationPager.value = viewModel.stationPager.value
                            viewModel.playerWaiting.value = false
                        } else {
                            viewModel.stationPager.value!!.isPlaying = service!!.station.isPlaying
                            viewModel.stationPager.value = viewModel.stationPager.value
                        }
                    }
                    viewModel.setViewedStation(
                        this@MainActivity,
                        pagerStations[position]
                    )
                }
            }
        })
        binding.playerView.playPause.setOnClickListener {
            if (service!=null) {
                if (service!!.station.id == viewModel.station.value!!.id) {
                    if (viewModel.station.value!!.isPlaying)
                        player.stop()
                    else {
                        viewModel.playerWaiting.value = true
                        player.prepare()
                        player.play()
                    }
                } else {
                    viewModel.playerWaiting.value = true
                    playStation(viewModel.station.value!!)
                }
            }
        }
        binding.playerView.playPauseExtended.setOnClickListener {
            if (service!=null) {
                if (service!!.station.id == viewModel.stationPager.value!!.id) {
                    if (viewModel.stationPager.value!!.isPlaying)
                        player.stop()
                    else {
                        viewModel.playerWaiting.value = true
                        player.prepare()
                        player.play()
                    }
                } else if (fromUser && AppData.arraysEqualsContent(
                        AppData.stationsPlayer,
                        ArrayList(pagerStations)
                    )
                ) {
                    viewModel.playerWaiting.value = true
                    val position = viewModel.getStationPosition(
                        viewModel.stationPager.value!!,
                        AppData.stationsPlayer
                    )
                    service!!.changeStation(viewModel.stationPager.value!!, position)
                } else {
                    viewModel.playerWaiting.value = true
                    playStation(viewModel.stationPager.value!!)
                }
            }
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
        }
        binding.playerView.recordExtended.setOnClickListener {
            if (viewModel.stationPager.value!!.isPlaying) {
                if (!viewModel.playerRecording.value!!)
                    checkPermissions()
                else {
                    stopRecord()
                }
            } else if (viewModel.playerRecording.value!!) stopRecord()
        }
        binding.playerView.playlistExtended.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("station", viewModel.stationPager.value!!)
            navController.navigate(R.id.menu_playlist, bundle)
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
                        if (service!=null&&service!!.station.name==""){
                            BottomSheetBehavior.from(binding.playerView.root).isHideable = true
                            BottomSheetBehavior.from(binding.playerView.root).state =
                                BottomSheetBehavior.STATE_HIDDEN
                        } else {
                            pagerStations.clear()
                            pagerStations.addAll(AppData.stationsPlayer)
                            playerStationsAdapter.submitList(AppData.stationsPlayer){
                                val position = viewModel.getStationPosition(viewModel.station.value!!,
                                    AppData.stationsPlayer)
                                binding.playerView.playerPager
                                    .setCurrentItem(position, false)
                            }
                            BottomSheetBehavior.from(binding.playerView.root).isHideable = false
                        }
                        binding.playerView.playerExpanded.visibility = View.GONE
                        hideKeyboard()
                        fromUser = false
                    } else
                    if (newState == BottomSheetBehavior.STATE_EXPANDED){
                        binding.playerView.playerMini.visibility = View.GONE
                        hideKeyboard()
                        if (AppData.getSettingBoolean(this@MainActivity,"autoplay")
                            &&service!=null&&service!!.station.id!=viewModel.stationPager.value!!.id) {
                            viewModel.playerWaiting.value = true
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

    @SuppressLint("NotifyDataSetChanged")
    fun showPlayer(stations: ArrayList<Station>){
        pagerStations.clear()
        pagerStations.addAll(stations)
        clickedPosition = viewModel
            .getStationPosition(viewModel.stationPager.value!!,
                stations)
        binding.playerView.playerExpanded.visibility = View.VISIBLE
        fromUser = false
        playerStationsAdapter.submitList(null)
        playerStationsAdapter.notifyDataSetChanged()
        playerStationsAdapter.submitList(stations){
            binding.playerView.playerPager
                .setCurrentItem(clickedPosition, false)
            BottomSheetBehavior.from(binding.playerView.root).state =
                BottomSheetBehavior.STATE_EXPANDED
            if (fromService) {
                fromService = false
                playerStationsAdapter.notifyItemChanged(clickedPosition)
            }
        }
        viewModel.loadAds.postValue(true)
    }

    private fun playStation(station: Station){
        Thread {
            //        viewModel.playerWaiting.value = true
            AppData.stationsPlayer.clear()
            AppData.stationsPlayer.addAll(pagerStations)
            AppData.stationsPlayer.forEach {
                it.isPlaying = false
                it.track = ""
            }
            viewModel.station.postValue(viewModel.stationPager.value)
            playerStationsAdapter.submitList(AppData.stationsPlayer) {
//            playerStationsAdapter.notifyItemChanged(viewModel
//                .getStationPosition(viewModel.station.value!!,AppData.stationsPlayer))
                val intent = Intent(this, PlayerService::class.java)
                val serviceBundle = Bundle()
                serviceBundle.putSerializable("station", station)
                serviceBundle.putBoolean("fromAlarm", false)
                intent.putExtra("bundle", serviceBundle)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else startService(intent)
                stopRecord()
            }

        }.start()
    }

    private fun ViewPager2.getRecycler(): RecyclerView?{
        try{
            val field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            return field.get(this) as RecyclerView
        }catch (e:Exception){e.printStackTrace()}
        return null
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
                "${getString(R.string.app_name)} needs permission.\n" +
                        "Press OK to continue.",
                282,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            recordAudio()
        }
    }

    private fun recordAudio() {
        viewModel.playerRecording.postValue(true)
        service?.recordAudio()
    }

    private fun stopRecord(){
        if (viewModel.playerRecording.value!!){
            viewModel.playerRecording.postValue(false)
            viewModel.recordTime.postValue("")
            service?.stopRecord()
            if (!isDestroyed) {
                DialogRecord(this,this).show()
            }
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
        if (requestCode==282) recordAudio()
    }

    override fun onResume() {
        super.onResume()
        try {
            binding.playerView.trackName.isSelected = true
        } catch (e:Exception){e.printStackTrace()}
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

    override fun onCopyClick(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("topradio", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    override fun onBitrateClick(position: Int) {
        try {
            if (service != null) {
                selectedBitrate = position
                if (viewModel.stationPager.value!!.isPlaying) {
                    service!!.setBitrate(position)
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
        } catch (e:Exception){}
    }

    override fun openFolder() {
        navController.navigate(R.id.menu_records)
    }

    fun showMenuDialog(){
        dialogMenu.show()
    }

    @UiThread
    private fun showInternetDialog(){
        if (!isDestroyed) {
            val dialog = DialogInternet(this)
            dialog.setOnDismissListener {
                hideKeyboard()
            }
            dialog.show()
        }
    }

    @UiThread
    private fun showStationOffDialog(){
        if (!isDestroyed) {
            val dialog = DialogStationOff(this)
            dialog.setOnDismissListener {
                hideKeyboard()
            }
            dialog.show()
        }
    }

    override fun onMenuPositionClick(position: Int) {
        when (position){
            0 -> navController.navigate(R.id.menu_alarm)
            1 -> openAddStation()
            2 -> openFeedback()
            3 -> navController.navigate(R.id.menu_settings)
            4 -> {
                service?.exitService()
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
        if (!isDestroyed) Glide.with(this).pauseAllRequests()
        super.onDestroy()
    }

}