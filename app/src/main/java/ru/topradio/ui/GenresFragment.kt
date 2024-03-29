package ru.topradio.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import ru.topradio.R
import ru.topradio.databinding.FragmentGenresBinding
import ru.topradio.model.Genre
import ru.topradio.ui.adapters.GenresListAdapter

class GenresFragment: Fragment(), GenresListAdapter.OnClickListener {

    private lateinit var binding: FragmentGenresBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_genres,
            container, false)
        binding.adapter = GenresListAdapter(this)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as MainActivity).viewModel.genres.observe(viewLifecycleOwner,{
            if (it!=null){
                binding.adapter!!.submitList(it) {
                    //binding.genresList.scrollToPosition(0)
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
                        (activity as MainActivity).viewModel.searchGenres(newText)
                    else if (newText.isEmpty()) (activity as MainActivity)
                        .viewModel.clearSearchGenres()
                    return true
                }

            })
            setOnCloseListener {
                (activity as MainActivity).viewModel.clearSearchGenres()
                onActionViewCollapsed()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) {
            (activity as MainActivity).navController.navigate(R.id.favorites)
            (activity as MainActivity).scrollToFirst = true
        }
        if (item.itemId==R.id.app_bar_menu)
            (activity as MainActivity).showMenuDialog()
        return super.onOptionsItemSelected(item)
    }

    override fun onGenreClick(genre: Genre) {
        (activity as MainActivity).scrollToFirst = true
        (activity as MainActivity).hideKeyboard()
        val bundle = Bundle()
        bundle.putInt("genreId", genre.id)
        bundle.putString("genreName", genre.name)
        (activity as MainActivity).navController.navigate(R.id.genresStations, bundle)
    }
}