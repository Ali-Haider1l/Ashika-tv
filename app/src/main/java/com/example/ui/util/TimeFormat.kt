package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    fun formatTime(millis: Long): String {
        if (millis <= 0L) return "--:--"
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun formatRange(startMillis: Long, endMillis: Long): String {
        return "${formatTime(startMillis)} – ${formatTime(endMillis)}"
    }

    fun formatRelativeUpdate(millis: Long): String {
        if (millis <= 0L) return "Not loaded"
        return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
    }
}
