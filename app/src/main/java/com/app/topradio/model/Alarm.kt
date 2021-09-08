package com.app.topradio.model

import java.io.Serializable

class Alarm: Serializable {
    var dateTime = 0L
    var repeat = HashSet<String>()
    var station = Station()
    var volumeSlide = false
}