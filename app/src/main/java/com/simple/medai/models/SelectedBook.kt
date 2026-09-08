package com.simple.medai.models

data class SelectedBook(
    val uri: String,
    val name: String,
    val sizeBytes: Long
) {
    val sizeLabel: String
        get() = when {
            sizeBytes >= 1024L * 1024L -> "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024L -> "%.1f KB".format(sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}
