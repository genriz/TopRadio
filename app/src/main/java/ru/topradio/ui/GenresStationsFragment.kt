package ru.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import ru.topradio.R
import ru.topradio.databinding.FragmentGenresStationsBinding
import ru.topradio.ui.adapters.StationsListAdapter
import ru.topradio.model.Station
import ru.topradio.ui.adapters.StationsListGridAdapter
import ru.topradio.util.AppData

class GenresStationsFragment: Fragment(), StationsListAdapter.OnClickListener,
    StationsListGridAdapter.OnClickListener {

    private lateinit var binding: FragmentGenresStationsBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_genres_stations, container, false)
        if (AppData.getSettingString(requireContext(),"view")
            ==requireContext().getString(R.string.list)){
            binding.stationsList.layoutManager = LinearLayoutManager(requireContext())
            binding.adapter = StationsListAdapter(requireContext(),this)
        } else {
            binding.stationsList.layoutManager = StaggeredGridLayoutManager(3,
                StaggeredGridLayoutManager.VERTICAL)
            binding.adapter = StationsListGridAdapter(this)
        }
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
                if ((activity as MainActivity).scrollToFirst) {
                    binding.adapter!!.submitList(ArrayList<Station>())
                    (activity as MainActivity).scrollToFirst = false
                }
                binding.adapter!!.submitList(it)
            }
        })

        (activity as MainActivity).viewModel.updateItemPosition.observe(viewLifecycleOwner,{
            it?.let{ position ->
                binding.adapter!!.notifyItemChanged(position)
            }
        })

        (activity as MainActivity).viewModel.getGenreStations(genreId)

    }

    override fun onStationClick(station: Station) {
        (activity as MainActivity).hideKeyboard()
        (activity as MainActivity).viewModel.setViewedStation(requireContext(), station)
        (activity as MainActivity).viewModel.stationPager.value = station
        (activity as MainActivity).showPlayer(true)
    }

    override fun onFavoriteClick(station: Station, position: Int) {
        station.isFavorite = !station.isFavorite
        binding.adapter!!.notifyItemChanged(position)
        (activity as MainActivity).viewModel.updateStationFavorite(requireContext(), station)
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
        if (item.itemId==R.id.app_bar_favorite) {
            (activity as MainActivity).navController.navigate(R.id.favorites)
            (activity as MainActivity).scrollToFirst = true
        }
        if (item.itemId==R.id.app_bar_menu)
            (activity as MainActivity).showMenuDialog()
        return super.onOptionsItemSelected(item)
    }

    override fun onDetach() {
        (activity as MainActivity).scrollToFirst = false
        (activity as MainActivity).viewModel.clearSearchStations()
        //(activity as MainActivity).updatePlayerPager()
        super.onDetach()
    }
}