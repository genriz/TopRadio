package com.app.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.topradio.R
import com.app.topradio.databinding.FragmentCitiesBinding
import com.app.topradio.model.City
import com.app.topradio.ui.adapters.CitiesListAdapter

class CitiesFragment: Fragment(), CitiesListAdapter.OnClickListener {

    private lateinit var binding: FragmentCitiesBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cities,
            container, false)
        binding.citiesList.layoutManager = LinearLayoutManager(requireContext())
        binding.adapter = CitiesListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as MainActivity).viewModel.cities.observe(viewLifecycleOwner,{
            if (it!=null){
                binding.adapter!!.submitList(it) {
                    //binding.citiesList.scrollToPosition(0)
                }
            }
        })

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
                        (activity as MainActivity).viewModel.searchCities(newText)
                    else if (newText.isEmpty()) (activity as MainActivity)
                        .viewModel.clearSearchCities()
                    return true
                }

            })
            setOnCloseListener {
                (activity as MainActivity).viewModel.clearSearchCities()
                onActionViewCollapsed()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity)
            .navController.navigate(R.id.favorites)
        if (item.itemId==R.id.app_bar_menu)
            (activity as MainActivity).showMenuDialog()
        return super.onOptionsItemSelected(item)
    }

    override fun onCityClick(city: City) {
        (activity as MainActivity).hideKeyboard()
        val bundle = Bundle()
        bundle.putInt("cityId", city.id)
        bundle.putString("cityName", city.name)
        (activity as MainActivity).navController.navigate(R.id.citiesStations, bundle)
    }

}