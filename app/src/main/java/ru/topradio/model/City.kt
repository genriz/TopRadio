package ru.topradio.model

import com.google.gson.annotations.SerializedName

class City {
    var id = 0
    @SerializedName("pa")
    var name = ""
    var count = 0
}