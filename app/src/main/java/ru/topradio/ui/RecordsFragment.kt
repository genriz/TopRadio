package ru.topradio.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions
import ru.topradio.R
import ru.topradio.databinding.FragmentRecordsBinding
import ru.topradio.model.Record
import ru.topradio.ui.adapters.RecordsListAdapter
import ru.topradio.util.AppData
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class RecordsFragment: Fragment(), RecordsListAdapter.OnClickListener,
    EasyPermissions.PermissionCallbacks{

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

        checkPermissions()
    }

    private fun readFolder(){

        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Environment.DIRECTORY_MUSIC + File.separator + "TopRadio"
        else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            .toString() + File.separator + "TopRadio"

        binding.recordsInfo.text = path
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            "${MediaStore.Audio.Media.RELATIVE_PATH} like ? "
        else MediaStore.Audio.Media.DATA + " like ? "

        val selectionArgs = arrayOf("${path}%")
        val records = ArrayList<Record>()

        requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            selection,
            selectionArgs,
            null
        )?.use {
            while (it.moveToNext()) {
                var icon = ""
                val fileNameIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                AppData.stations.forEach { station ->
                    if (station.name==it.getString(fileNameIndex).substringBeforeLast("_"))
                        icon = station.icon
                }
                val record = Record().apply {
                    id = records.size
                    name = it.getString(fileNameIndex)
                    logo = icon
                    date = (it.getString(fileNameIndex).substringAfterLast("_")
                        .substringBefore(".")).toLong()
                    uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it.getInt(it.getColumnIndex(MediaStore.MediaColumns._ID)).toString())
                }
                records.add(record)
            }
            records.sortByDescending {record -> record.date }
            binding.adapter!!.submitList(records)
        }
    }

    private fun checkPermissions() {
        if (!EasyPermissions.hasPermissions(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            EasyPermissions.requestPermissions(
                this,
                "${requireContext().getString(R.string.app_name)} needs permission.\n" +
                        "Press OK to continue.",
                283,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            readFolder()
        }
    }

    private fun openFile(uri: Uri){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "audio/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        requireContext().startActivity(intent)
    }

    override fun onRecordClick(record: Record) {
        openFile(record.uri!!)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode==283) readFolder()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }
}