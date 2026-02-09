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
    fun createPlaylist(@RequestBody request: PlaylistCreateRequest): ResponseEntity<Playlist> {
        val playlist = playlistService.createPlaylist(request.name, request.songIds)
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist)
    }

    @PutMapping("/{playlistId}")
    fun updatePlaylistInfo(
        @PathVariable playlistId: String,
        @RequestBody request: PlaylistInfoUpdateRequest
    ): ResponseEntity<Void> {
        val updated = playlistService.updatePlaylistInfo(playlistId, request.name)
        return if (updated) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{playlistId}/songs")
    fun updatePlaylistSongs(
        @PathVariable playlistId: String,
        @RequestBody request: PlaylistSongsUpdateRequest
    ): ResponseEntity<Void> {
        val updated = playlistService.updatePlaylistSongs(playlistId, request.songIds)
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
}

