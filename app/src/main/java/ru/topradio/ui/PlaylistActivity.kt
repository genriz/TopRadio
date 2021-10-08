package ru.topradio.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import ru.topradio.R
import ru.topradio.databinding.ActivityPlaylistsBinding
import ru.topradio.model.MainViewModel
import ru.topradio.model.PlaylistItem
import ru.topradio.model.State
import ru.topradio.model.Station
import ru.topradio.ui.adapters.PlayListAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PlaylistActivity : AppCompatActivity(), PlayListAdapter.OnClick {

    private lateinit var binding: ActivityPlaylistsBinding
    private val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_playlists)
        binding.adapter = PlayListAdapter(this)
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val station = intent?.extras?.getSerializable("station") as Station

        supportActionBar?.title = station.name

        viewModel.playlistApi.value = null
        binding.progressPlaylist.visibility = View.VISIBLE
        binding.btnTop100.isSelected = false
        binding.btnPlaylist.isSelected = true

        binding.btnPlaylist.setOnClickListener {
            if (!binding.btnPlaylist.isSelected){
                viewModel.playlistApi.value = null
                binding.adapter!!.submitList(ArrayList<PlaylistItem>())
                binding.progressPlaylist.visibility = View.VISIBLE
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().timeInMillis)
                viewModel
                    .getPlaylist(station.playList!!,
                        date)
                binding.btnTop100.isSelected = false
                binding.btnPlaylist.isSelected = true
            }
        }
        binding.btnTop100.setOnClickListener {
            if (!binding.btnTop100.isSelected){
                viewModel.playlistApi.value = null
                binding.adapter!!.submitList(ArrayList<PlaylistItem>())
                binding.progressPlaylist.visibility = View.VISIBLE
                viewModel
                    .getTop100(station.playList!!)
                binding.btnTop100.isSelected = true
                binding.btnPlaylist.isSelected = false
            }
        }

        viewModel.playlist.observe(this,{
            if (it!=null){
                binding.adapter!!.submitList(it)
                binding.progressPlaylist.visibility = View.GONE
            }
        })

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().timeInMillis)
        viewModel.getPlaylist(station.playList!!, date)

        viewModel.state.observe(this,{
            if (it!=null&&it== State.STATE_FAILED) binding.progressPlaylist.visibility = View.GONE
        })
    }

    override fun onCopyClick(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("topradio", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}