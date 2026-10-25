package com.lumen.ultralight.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).playlistDao()

    val playlists = dao.playlistsWithCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun trackIds(playlistId: Long): Flow<List<Long>> = dao.trackIds(playlistId)

    fun create(name: String) = viewModelScope.launch {
        dao.insertPlaylist(PlaylistEntity(name = name, position = dao.nextPlaylistPosition()))
    }

    fun createAndAdd(name: String, trackId: Long) = viewModelScope.launch {
        val id = dao.insertPlaylist(PlaylistEntity(name = name, position = dao.nextPlaylistPosition()))
        dao.addTrack(PlaylistTrackEntity(playlistId = id, trackId = trackId, position = 0))
    }

    fun rename(id: Long, name: String) = viewModelScope.launch { dao.rename(id, name) }

    fun delete(id: Long) = viewModelScope.launch {
        dao.deletePlaylistTracks(id)
        dao.deletePlaylist(id)
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        dao.addTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = dao.nextTrackPosition(playlistId),
            )
        )
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        dao.removeTrack(playlistId, trackId)
    }
}
