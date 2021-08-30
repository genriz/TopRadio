package com.app.topradio.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentCityStationsBinding
import com.app.topradio.ui.adapters.StationsListAdapter
import com.app.topradio.model.Station

class CityStationsFragment: Fragment(), StationsListAdapter.OnClickListener {

    private lateinit var binding: FragmentCityStationsBinding
    private lateinit var searchView: SearchView
    private lateinit var menu: Menu

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_city_stations, container, false)
        binding.adapter = StationsListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val citiId = requireArguments().get("cityId") as Int
        val cityName = requireArguments().get("cityName") as String

        (activity as MainActivity).supportActionBar!!.title = cityName
        (activity as MainActivity).supportActionBar!!.setIcon(null)

        (activity as MainActivity).viewModel.stations.observe(viewLifecycleOwner,{
            if (it!=null){
                binding.adapter!!.submitList(it)
            }
        })

        (activity as MainActivity).viewModel.updateItemPosition.observe(viewLifecycleOwner,{
            it?.let{ position ->
                binding.adapter!!.notifyItemChanged(position)
            }
        })

        (activity as MainActivity).viewModel.getCityStations(citiId)

    }

    override fun onStationClick(station: Station) {
        (activity as MainActivity).hideKeyboard()
        (activity as MainActivity).viewModel.station.value = station
        (activity as MainActivity).viewModel.stationsApi.value!!.forEach { it.isPlaying = false }
        (activity as MainActivity).showPlayer(true)
        if (!searchView.isIconified) {
            searchView.onActionViewCollapsed()
        }
    }

    override fun onFavoriteClick(station: Station, position: Int) {
        station.isFavorite = !station.isFavorite
        binding.adapter!!.notifyItemChanged(position)
        (activity as MainActivity).viewModel.updateStation(requireContext(), station)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
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
                    else if (newText.isEmpty())
                        (activity as MainActivity).viewModel.clearSearchStations()
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
        if (item.itemId==android.R.id.home) {
            searchView.onActionViewCollapsed()
            (activity as MainActivity).viewModel.clearSearchCities()
        }
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity)
            .navController.navigate(R.id.favorites)
        return super.onOptionsItemSelected(item)
    }

    override fun onDetach() {
        (activity as MainActivity).viewModel.clearSearchCities()
        super.onDetach()
    }
}