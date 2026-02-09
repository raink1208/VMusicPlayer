package com.github.rain1208.vmusicplayerserver.services

import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail
import com.github.rain1208.vmusicplayerserver.repository.PlaylistRepository
import com.github.rain1208.vmusicplayerserver.utils.ULIDGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlaylistService(private val playlistRepository: PlaylistRepository, private val ulidGenerator: ULIDGenerator) {
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

    fun createPlaylist(name: String, songIds: List<String>): Playlist {
        logger.info("Creating playlist: name={}, songCount={}", name, songIds.size)
        val playlistId = ulidGenerator.generate()
        val playlist = playlistRepository.createPlaylist(playlistId, name)

        // 楽曲が指定されている場合は追加
        if (songIds.isNotEmpty()) {
            playlistRepository.updatePlaylistSongs(playlistId, songIds)
        }

        logger.info("Playlist created successfully: playlistId={}", playlist.playlistId)
        return playlist
    }

    fun updatePlaylistInfo(playlistId: String, name: String): Boolean {
        logger.info("Updating playlist info: playlistId={}, name={}", playlistId, name)
        val updated = playlistRepository.updatePlaylistInfo(playlistId, name)
        if (updated) {
            logger.info("Playlist info updated successfully: playlistId={}", playlistId)
        } else {
            logger.warn("Failed to update playlist info: playlistId={}", playlistId)
        }
        return updated
    }

    fun updatePlaylistSongs(playlistId: String, songIds: List<String>): Boolean {
        logger.info("Updating playlist songs: playlistId={}, songCount={}", playlistId, songIds.size)
        val updated = playlistRepository.updatePlaylistSongs(playlistId, songIds)
        if (updated) {
            logger.info("Playlist songs updated successfully: playlistId={}, songCount={}", playlistId, songIds.size)
        } else {
            logger.warn("Failed to update playlist songs: playlistId={}", playlistId)
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
}

