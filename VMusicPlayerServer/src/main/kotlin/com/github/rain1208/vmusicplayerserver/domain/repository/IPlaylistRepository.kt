package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail

interface IPlaylistRepository {
    fun getAllPlaylists(): List<Playlist>
    fun getPlaylist(playlistId: String): PlaylistDetail?
    fun createPlaylist(name: String): Playlist
    fun updatePlaylistName(playlistId: String, name: String): Boolean
    fun deletePlaylist(playlistId: String): Boolean
    fun addSongToPlaylist(playlistId: String, songId: String): Int // returns position
    fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean
    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>): Boolean
    fun createPlaylistFromQueue(name: String, songIds: List<String>): Playlist
}