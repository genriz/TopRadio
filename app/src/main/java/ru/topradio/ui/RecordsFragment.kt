package ru.topradio.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import ru.topradio.R
import ru.topradio.databinding.FragmentRecordsBinding
import ru.topradio.model.Record
import ru.topradio.ui.adapters.RecordsListAdapter
import ru.topradio.util.AppData
import java.io.File

class RecordsFragment: Fragment(), RecordsListAdapter.OnClickListener {

    private lateinit var binding: FragmentRecordsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_records, container, false)
        binding.adapter = RecordsListAdapter(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val path = Environment.getExternalStorageDirectory().path + "/TopRadio"
        binding.recordsInfo.text = path

        val folder = File(Environment.getExternalStorageDirectory(), "TopRadio")
        folder.exists().let {
            if (it){
                val records = ArrayList<Record>()
                folder.listFiles()?.forEach { file ->
                    var icon = ""
                    AppData.stations.forEach { station ->
                        if (station.name==file.name.substringBeforeLast("_"))
                            icon = station.icon
                    }
                    val record = Record().apply {
                        id = records.size
                        name = file.name
                        logo = icon
                        date = (file.name.substringAfterLast("_").substringBefore(".")).toLong()
                    }
                    records.add(record)
                }
                records.sortByDescending {record -> record.date }
                binding.adapter!!.submitList(records)
            }
        }
    }

    private fun openFile(file: String){
        val uri = FileProvider.getUriForFile(requireContext(),
            requireContext().applicationContext.packageName + ".provider",
            File(Environment.getExternalStorageDirectory(), "/TopRadio/$file"))
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "audio/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        requireContext().startActivity(intent)
    }

    override fun onRecordClick(record: Record) {
        openFile(record.name)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.app_bar_search).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.app_bar_favorite) (activity as MainActivity)
            .navController.navigate(R.id.favorites)
        if (item.itemId==R.id.app_bar_menu)
            (activity as MainActivity).showMenuDialog()
        return super.onOptionsItemSelected(item)
    }
}