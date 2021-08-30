package com.app.topradio.ui

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentHomeBinding
import com.app.topradio.ui.adapters.StationsListAdapter
import com.app.topradio.model.Station
import com.app.topradio.util.AppData

class HomeFragment: Fragment(), StationsListAdapter.OnClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.adapter = StationsListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as MainActivity).viewModel.stations.observe(viewLifecycleOwner,{
            if (it!=null){
                it.forEach { station ->
                    if (AppData.favorites.contains(station.id.toString()))
                        station.isFavorite = true
                }
                binding.adapter!!.submitList(it)
                (activity as MainActivity).updatePlayerPager()
            }
        })

        (activity as MainActivity).viewModel.updateItemPosition.observe(viewLifecycleOwner,{
            it?.let{ position ->
                binding.adapter!!.notifyItemChanged(position)
            }
        })

        (activity as MainActivity).viewModel.getAllStations()

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(requireActivity(), object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!searchView.isIconified) {
                        searchView.onActionViewCollapsed()
                    } else {
                        remove()
                        activity?.onBackPressed()
                    }
                }
            })
    }

    override fun onStationClick(station: Station) {
        (activity as MainActivity).hideKeyboard()
        (activity as MainActivity).viewModel.station.value = station
        (activity as MainActivity).viewModel.stationPager.value = station
        (activity as MainActivity).viewModel.stationsApi.value!!.forEach { it.isPlaying = false }
        (activity as MainActivity).showPlayer(false)
    }

    override fun onFavoriteClick(station: Station, position: Int) {
        station.isFavorite = !station.isFavorite
        binding.adapter!!.notifyItemChanged(position)
        (activity as MainActivity).viewModel.updateStation(requireContext(), station)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
                        (activity as MainActivity).viewModel.searchStations(newText)
                    else if (newText.isEmpty()) (activity as MainActivity)
                        .viewModel.clearSearchStations()
                    return true
                }

            })
            setOnCloseListener {
                (activity as MainActivity).viewModel.clearSearchStations()
                onActionViewCollapsed()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity)
            .navController.navigate(R.id.favorites)
        return super.onOptionsItemSelected(item)
    }
}