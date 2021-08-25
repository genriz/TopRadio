package com.app.topradio.util

import android.app.Activity
import android.content.Context
import okhttp3.Credentials

object AppData {

    val auth = Credentials.basic("goword", "0GdouNEOMd")

    fun getFavorites(context: Context): HashSet<String>{
        return (context.getSharedPreferences("prefs", Activity.MODE_PRIVATE)
            .getStringSet("favorites", HashSet<String>()) as HashSet<String>?)!!
    }
}