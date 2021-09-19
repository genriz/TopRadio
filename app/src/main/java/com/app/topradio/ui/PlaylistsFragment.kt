package com.app.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentPlaylistsBinding
import com.app.topradio.model.PlaylistItem
import com.app.topradio.model.State
import com.app.topradio.ui.adapters.PlayListAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PlaylistsFragment: Fragment() {

    private lateinit var binding: FragmentPlaylistsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_playlists,
            container, false)
        binding.adapter = PlayListAdapter()
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).viewModel.playlistApi.value = null
        binding.progressPlaylist.visibility = View.VISIBLE
        binding.btnTop100.isSelected = false
        binding.btnPlaylist.isSelected = true

        binding.btnPlaylist.setOnClickListener {
            if (!binding.btnPlaylist.isSelected){
                (activity as MainActivity).viewModel.playlistApi.value = null
                binding.adapter!!.submitList(ArrayList<PlaylistItem>())
                binding.progressPlaylist.visibility = View.VISIBLE
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().timeInMillis)
                (activity as MainActivity).viewModel
                    .getPlaylist((activity as MainActivity).viewModel.stationPager.value!!.playList!!,
                        date)
                binding.btnTop100.isSelected = false
                binding.btnPlaylist.isSelected = true
            }
        }
        binding.btnTop100.setOnClickListener {
            if (!binding.btnTop100.isSelected){
                (activity as MainActivity).viewModel.playlistApi.value = null
                binding.adapter!!.submitList(ArrayList<PlaylistItem>())
                binding.progressPlaylist.visibility = View.VISIBLE
                (activity as MainActivity).viewModel
                    .getTop100((activity as MainActivity).viewModel.stationPager.value!!.playList!!)
                binding.btnTop100.isSelected = true
                binding.btnPlaylist.isSelected = false
            }
        }

        (activity as MainActivity).viewModel.playlist.observe(viewLifecycleOwner,{
            if (it!=null){
                binding.adapter!!.submitList(it)
                binding.progressPlaylist.visibility = View.GONE
            }
        })

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().timeInMillis)
        (activity as MainActivity).viewModel
            .getPlaylist((activity as MainActivity).viewModel.stationPager.value!!.playList!!,
                date)

        (activity as MainActivity).viewModel.state.observe(viewLifecycleOwner,{
            if (it!=null&&it== State.STATE_FAILED) binding.progressPlaylist.visibility = View.GONE
        })
    }

    override fun onDetach() {
        (activity as MainActivity).toExpandedPlayer()
        super.onDetach()
    }

}