package ru.topradio.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import ru.topradio.R
import ru.topradio.databinding.FragmentPlaylistsBinding
import ru.topradio.model.PlaylistItem
import ru.topradio.model.State
import ru.topradio.ui.adapters.PlayListAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PlaylistsFragment: Fragment(), PlayListAdapter.OnClick {

    private lateinit var binding: FragmentPlaylistsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_playlists,
            container, false)
        binding.adapter = PlayListAdapter(this)
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

        (activity as MainActivity).viewModel.playlist.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.adapter!!.submitList(it)
                binding.progressPlaylist.visibility = View.GONE
            }
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().timeInMillis)
        (activity as MainActivity).viewModel
            .getPlaylist((activity as MainActivity).viewModel.stationPager.value!!.playList!!,
                date)

        (activity as MainActivity).viewModel.state.observe(viewLifecycleOwner) {
            if (it != null && it == State.STATE_FAILED) binding.progressPlaylist.visibility =
                View.GONE
        }
    }

//    override fun onDetach() {
//        (activity as MainActivity).toExpandedPlayer()
//        super.onDetach()
//    }

    override fun onCopyClick(text: String) {
        (requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("topradio", text))
        Toast.makeText(requireActivity(), R.string.copied, Toast.LENGTH_SHORT).show()
    }

}