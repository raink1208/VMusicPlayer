package com.github.rain1208.vmusicplayerserver.controller

import com.github.rain1208.vmusicplayerserver.controller.dto.*
import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail
import com.github.rain1208.vmusicplayerserver.services.PlaylistService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/playlists")
class PlaylistController(private val playlistService: PlaylistService) {

    @GetMapping
    fun getAllPlaylists(): ResponseEntity<List<Playlist>> {
        val playlists = playlistService.getAllPlaylists()
        return ResponseEntity.ok(playlists)
    }

    @GetMapping("/{playlistId}")
    fun getPlaylist(@PathVariable playlistId: String): ResponseEntity<PlaylistDetail> {
        val playlist = playlistService.getPlaylist(playlistId)
        return if (playlist != null) {
            ResponseEntity.ok(playlist)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createPlaylist(@RequestBody request: CreatePlaylistRequest): ResponseEntity<Playlist> {
        val playlist = playlistService.createPlaylist(request.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist)
    }

    @PutMapping("/{playlistId}")
    fun updatePlaylistName(
        @PathVariable playlistId: String,
        @RequestBody request: UpdatePlaylistNameRequest
    ): ResponseEntity<Void> {
        val updated = playlistService.updatePlaylistName(playlistId, request.name)
        return if (updated) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{playlistId}")
    fun deletePlaylist(@PathVariable playlistId: String): ResponseEntity<Void> {
        val deleted = playlistService.deletePlaylist(playlistId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{playlistId}/songs")
    fun addSongToPlaylist(
        @PathVariable playlistId: String,
        @RequestBody request: AddSongToPlaylistRequest
    ): ResponseEntity<Map<String, Int>> {
        val position = playlistService.addSongToPlaylist(playlistId, request.songId)
        return ResponseEntity.ok(mapOf("position" to position))
    }

    @DeleteMapping("/{playlistId}/songs/{songId}")
    fun removeSongFromPlaylist(
        @PathVariable playlistId: String,
        @PathVariable songId: String
    ): ResponseEntity<Void> {
        val removed = playlistService.removeSongFromPlaylist(playlistId, songId)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{playlistId}/songs/reorder")
    fun reorderPlaylistSongs(
        @PathVariable playlistId: String,
        @RequestBody request: ReorderPlaylistSongsRequest
    ): ResponseEntity<Void> {
        val reordered = playlistService.reorderPlaylistSongs(playlistId, request.songIds)
        return if (reordered) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/from-queue")
    fun createPlaylistFromQueue(
        @RequestBody request: CreatePlaylistFromQueueRequest
    ): ResponseEntity<Playlist> {
        val playlist = playlistService.createPlaylistFromQueue(request.name, request.songIds)
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist)
    }
}

