package com.lumen.ultralight.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lumen.ultralight.model.Track

/**
 * Reads the device's music library straight from MediaStore, the same source a
 * normal Android player uses (so it covers the standard Music folder and anything
 * else the system has indexed). Query shape mirrors the logic proven in the
 * lumen-android MediaStore plugin.
 */
class MediaStoreScanner(private val context: Context) {

    fun scan(): List<Track> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE ASC, " +
            "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE ASC, " +
            "${MediaStore.Audio.Media.TRACK} ASC"

        val albumArtBase = Uri.parse("content://media/external/audio/albumart")
        val out = ArrayList<Track>()

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val rawTrack = if (c.isNull(trackCol)) 0 else c.getInt(trackCol)
                val trackNo = rawTrack % 1000 // MediaStore packs disc*1000 + track

                var artist = c.getString(artistCol) ?: ""
                if (artist == MediaStore.UNKNOWN_STRING) artist = ""

                out.add(
                    Track(
                        id = id,
                        title = c.getString(titleCol) ?: "",
                        artist = artist,
                        album = c.getString(albumCol) ?: "",
                        albumId = albumId,
                        durationMs = if (c.isNull(durationCol)) 0L else c.getLong(durationCol),
                        trackNo = trackNo,
                        uri = ContentUris.withAppendedId(collection, id),
                        artUri = if (albumId > 0) ContentUris.withAppendedId(albumArtBase, albumId) else null,
                    )
                )
            }
        }
        return out
    }
}
