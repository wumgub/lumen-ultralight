package com.lumen.ultralight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        "SELECT p.id AS id, p.name AS name, p.position AS position, COUNT(pt.rowId) AS count " +
            "FROM playlists p LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.position ASC, p.id ASC"
    )
    fun playlistsWithCounts(): Flow<List<PlaylistWithCount>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun deletePlaylistTracks(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlists")
    suspend fun nextPlaylistPosition(): Int

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :id ORDER BY position ASC, rowId ASC")
    fun trackIds(id: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id")
    suspend fun nextTrackPosition(id: Long): Int

    @Insert
    suspend fun addTrack(entry: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)
}
