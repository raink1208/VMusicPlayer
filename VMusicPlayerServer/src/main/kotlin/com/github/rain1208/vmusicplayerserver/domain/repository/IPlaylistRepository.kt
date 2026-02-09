package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail

interface IPlaylistRepository {
    fun getAllPlaylists(): List<Playlist>
    fun getPlaylist(playlistId: String): PlaylistDetail?
    fun createPlaylist(playlistId: String, name: String): Playlist
    fun updatePlaylistInfo(playlistId: String, name: String): Boolean
    fun updatePlaylistSongs(playlistId: String, songIds: List<String>): Boolean
    fun deletePlaylist(playlistId: String): Boolean
}