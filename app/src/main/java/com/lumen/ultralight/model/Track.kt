package com.lumen.ultralight.model

import android.net.Uri

/** One audio track from the device's MediaStore. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int,
    val uri: Uri,
    val artUri: Uri?,
)
