package com.app.topradio.ui

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.topradio.R
import com.app.topradio.databinding.FragmentFavoritesBinding
import com.app.topradio.ui.adapters.StationsListAdapter
import com.app.topradio.model.Station

class FavoritesFragment: Fragment(), StationsListAdapter.OnClickListener {

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var searchView: SearchView
    private val favorites = ArrayList<Station>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false)
        binding.adapter = StationsListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as MainActivity).viewModel.stations.observe(viewLifecycleOwner,{
            if (it!=null){
                favorites.clear()
                binding.adapter!!.notifyDataSetChanged()
                it.forEach { station ->
                    if (station.isFavorite) favorites.add(station)
                }
                binding.adapter!!.submitList(favorites) {
                    binding.stationsList.scrollToPosition(0)
                }
            }
        })

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(requireActivity(), object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!searchView.isIconified) {
                        searchView.isIconified = true
                    } else {
                        remove()
                        requireActivity().onBackPressed()
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
        inflater.inflate(R.menu.favorites_menu, menu)
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
                        (activity as MainActivity).viewModel.searchFavoritesStations(newText)
                    else if (newText.isEmpty())
                        (activity as MainActivity).viewModel.clearSearch()
                    return true
                }

            })
            setOnCloseListener {
                (activity as MainActivity).viewModel.clearSearch()
                onActionViewCollapsed()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity).onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}