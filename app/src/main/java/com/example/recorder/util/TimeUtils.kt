package com.example.recorder.util

fun Long.toFormattedDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun Long.toFormattedSize(): String = when {
    this >= 1_048_576 -> "%.1f MB".format(this / 1_048_576.0)
    this >= 1024 -> "%.0f KB".format(this / 1024.0)
    else -> "$this B"
}
