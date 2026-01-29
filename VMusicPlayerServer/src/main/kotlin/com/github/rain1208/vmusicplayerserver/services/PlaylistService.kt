package com.github.rain1208.vmusicplayerserver.services

import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail
import com.github.rain1208.vmusicplayerserver.repository.PlaylistRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlaylistService(private val playlistRepository: PlaylistRepository) {
    private val logger = LoggerFactory.getLogger(PlaylistService::class.java)

    fun getAllPlaylists(): List<Playlist> {
        logger.info("Fetching all playlists")
        val playlists = playlistRepository.getAllPlaylists()
        logger.info("Retrieved {} playlists", playlists.size)
        return playlists
    }

    fun getPlaylist(playlistId: String): PlaylistDetail? {
        logger.info("Fetching playlist: playlistId={}", playlistId)
        val playlist = playlistRepository.getPlaylist(playlistId)
        if (playlist != null) {
            logger.info("Playlist found: playlistId={}, name={}, songs={}",
                playlistId, playlist.name, playlist.songs.size)
        } else {
            logger.warn("Playlist not found: playlistId={}", playlistId)
        }
        return playlist
    }

    fun createPlaylist(name: String): Playlist {
        logger.info("Creating playlist: name={}", name)
        val playlist = playlistRepository.createPlaylist(name)
        logger.info("Playlist created successfully: playlistId={}", playlist.playlistId)
        return playlist
    }

    fun updatePlaylistName(playlistId: String, name: String): Boolean {
        logger.info("Updating playlist name: playlistId={}, newName={}", playlistId, name)
        val updated = playlistRepository.updatePlaylistName(playlistId, name)
        if (updated) {
            logger.info("Playlist name updated successfully: playlistId={}", playlistId)
        } else {
            logger.warn("Failed to update playlist name: playlistId={}", playlistId)
        }
        return updated
    }

    fun deletePlaylist(playlistId: String): Boolean {
        logger.info("Deleting playlist: playlistId={}", playlistId)
        val deleted = playlistRepository.deletePlaylist(playlistId)
        if (deleted) {
            logger.info("Playlist deleted successfully: playlistId={}", playlistId)
        } else {
            logger.warn("Failed to delete playlist: playlistId={}", playlistId)
        }
        return deleted
    }

    fun addSongToPlaylist(playlistId: String, songId: String): Int {
        logger.info("Adding song to playlist: playlistId={}, songId={}", playlistId, songId)
        val position = playlistRepository.addSongToPlaylist(playlistId, songId)
        logger.info("Song added to playlist at position {}: playlistId={}, songId={}",
            position, playlistId, songId)
        return position
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean {
        logger.info("Removing song from playlist: playlistId={}, songId={}", playlistId, songId)
        val removed = playlistRepository.removeSongFromPlaylist(playlistId, songId)
        if (removed) {
            logger.info("Song removed from playlist: playlistId={}, songId={}", playlistId, songId)
        } else {
            logger.warn("Failed to remove song from playlist: playlistId={}, songId={}",
                playlistId, songId)
        }
        return removed
    }

    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>): Boolean {
        logger.info("Reordering playlist songs: playlistId={}, songCount={}",
            playlistId, songIds.size)
        val reordered = playlistRepository.reorderPlaylistSongs(playlistId, songIds)
        if (reordered) {
            logger.info("Playlist songs reordered successfully: playlistId={}", playlistId)
        } else {
            logger.warn("Failed to reorder playlist songs: playlistId={}", playlistId)
        }
        return reordered
    }

    fun createPlaylistFromQueue(name: String, songIds: List<String>): Playlist {
        logger.info("Creating playlist from queue: name={}, songCount={}", name, songIds.size)
        val playlist = playlistRepository.createPlaylistFromQueue(name, songIds)
        logger.info("Playlist created from queue: playlistId={}, songCount={}",
            playlist.playlistId, playlist.songCount)
        return playlist
    }
}

