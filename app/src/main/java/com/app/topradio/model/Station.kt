package com.app.topradio.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Station: Serializable {
    var id = 0
    @SerializedName("me")
    var position = 0
    @SerializedName("pa")
    var name = ""
    @SerializedName("im")
    var icon = ""
    @SerializedName("st")
    var bitrates = ArrayList<Bitrate>()
    @SerializedName("ci")
    var cities = ArrayList<Int>()
    @SerializedName("ge")
    var genres = ArrayList<Int>()
    @SerializedName("pl")
    var playList = ""
    var isFavorite = false
    var isPlaying = false
    var isViewed = false
    var track = ""
    var isRecording = false
}

class Bitrate: Serializable {
    @SerializedName("bi")
    var bitrate = 0
    @SerializedName("ur")
    var url = ""
    var isSelected = false
}