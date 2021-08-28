package com.app.topradio.ui

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentGenresStationsBinding
import com.app.topradio.ui.adapters.StationsListAdapter
import com.app.topradio.model.Station

class GenresStationsFragment: Fragment(), StationsListAdapter.OnClickListener {

    private lateinit var binding: FragmentGenresStationsBinding
    private lateinit var searchView: SearchView
    private val cityStations = ArrayList<Station>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_genres_stations, container, false)
        binding.adapter = StationsListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val genreId = requireArguments().get("genreId") as Int
        val genreName = requireArguments().get("genreName") as String

        (activity as MainActivity).supportActionBar!!.title = genreName
        (activity as MainActivity).supportActionBar!!.setIcon(null)

        (activity as MainActivity).viewModel.stations.observe(viewLifecycleOwner,{
            if (it!=null){
                cityStations.clear()
                binding.adapter!!.notifyDataSetChanged()
                it.forEach { station ->
                    if (station.genres.contains(genreId)) cityStations.add(station)
                }
                binding.adapter!!.submitList(cityStations) {
                    binding.stationsList.scrollToPosition(0)
                }
            }
        })

    }

    override fun onStationClick(station: Station) {
        (activity as MainActivity).viewModel.station.value = station
        (activity as MainActivity).showPlayer()
        if (!searchView.isIconified) {
            searchView.onActionViewCollapsed()
        }
    }

    override fun onFavoriteClick(station: Station, position: Int) {
        station.isFavorite = !station.isFavorite
        binding.adapter!!.notifyItemRemoved(position)
        (activity as MainActivity).viewModel.stationsApi.value =
            (activity as MainActivity).viewModel.stations.value
        if (station.id==(activity as MainActivity).viewModel.station.value!!.id){
            (activity as MainActivity).viewModel.station.value =
                (activity as MainActivity).viewModel.station.value
        }
        val favorites = HashSet<String>()
        (activity as MainActivity).viewModel.stations.value!!.forEach {
            if (it.isFavorite){
                favorites.add(it.id.toString())
            }
        }
        requireContext().getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .edit().putStringSet("favorites", favorites).apply()
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
            (activity as MainActivity).viewModel.clearSearchStations()
        }
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity)
            .navController.navigate(R.id.favorites)
        return super.onOptionsItemSelected(item)
    }

    override fun onDetach() {
        (activity as MainActivity).viewModel.clearSearchGenres()
        super.onDetach()
    }
}