package com.lumen.ultralight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(tableName = "playlist_tracks", indices = [Index("playlistId")])
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

/** A playlist plus its track count, for the playlists list. */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val position: Int,
    val count: Int,
)
