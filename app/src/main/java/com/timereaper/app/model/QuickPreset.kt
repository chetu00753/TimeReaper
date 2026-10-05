package com.timereaper.app.model

enum class QuickPreset(val label: String, val minutes: Int) {
    FIFTEEN("15 min", 15),
    THIRTY("30 min", 30),
    FORTY_FIVE("45 min", 45),
    ONE_HOUR("1 hour", 60),
    TWO_HOURS("2 hours", 120)
}
