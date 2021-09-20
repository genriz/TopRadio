package ru.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import ru.topradio.R
import ru.topradio.databinding.FragmentFavoritesBinding
import ru.topradio.ui.adapters.StationsListAdapter
import ru.topradio.model.Station
import ru.topradio.ui.adapters.StationsListGridAdapter
import ru.topradio.util.AppData

class FavoritesFragment: Fragment(), StationsListAdapter.OnClickListener,
    StationsListGridAdapter.OnClickListener {

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false)
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

        (activity as MainActivity).viewModel.stationsFavorites.observe(viewLifecycleOwner,{
            if (it!=null){
                if ((activity as MainActivity).scrollToFirst) {
                    binding.adapter!!.submitList(ArrayList<Station>())
                    (activity as MainActivity).scrollToFirst = false
                }
                binding.adapter!!.submitList(it)
            }
        })

        (activity as MainActivity).viewModel.getFavoriteStations()

//        requireActivity()
//            .onBackPressedDispatcher
//            .addCallback(requireActivity(), object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    if (!searchView.isIconified) {
//                        searchView.isIconified = true
//                    } else {
//                        remove()
//                        activity?.onBackPressed()
//                    }
//                }
//            })
    }

    override fun onStationClick(station: Station) {
        (activity as MainActivity).hideKeyboard()
        (activity as MainActivity).viewModel.setViewedStation(requireContext(), station)
        (activity as MainActivity).viewModel.stationPager.value = station
        (activity as MainActivity).showPlayer(false)
    }

    override fun onFavoriteClick(station: Station, position: Int) {
        station.isFavorite = !station.isFavorite
        (activity as MainActivity).viewModel.updateStationFavorite(requireContext(), station)
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
                        (activity as MainActivity).viewModel.clearSearchStationsFavorites()
                    return true
                }

            })
            setOnCloseListener {
                (activity as MainActivity).viewModel.clearSearchStationsFavorites()
                onActionViewCollapsed()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity).onBackPressed()
        if (item.itemId==android.R.id.home) {
            searchView.onActionViewCollapsed()
            (activity as MainActivity).viewModel.clearSearchStationsFavorites()
        }
        if (item.itemId==R.id.app_bar_menu)
            (activity as MainActivity).showMenuDialog()
        return super.onOptionsItemSelected(item)
    }

    override fun onDetach() {
        (activity as MainActivity).viewModel.clearSearchStationsFavorites()
        super.onDetach()
    }
}