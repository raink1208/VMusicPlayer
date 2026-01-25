package com.github.rain1208.vmusicplayerserver.controller

import com.github.rain1208.vmusicplayerserver.controller.dto.RegisterSongRequest
import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.services.SongService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/songs")
class SongController(private val songService: SongService) {

    @GetMapping
    fun getAllSongs(): ResponseEntity<List<Song>> {
        val songs = songService.getAllSongs()
        return ResponseEntity.ok(songs)
    }

    @GetMapping("/{songId}")
    fun getSong(@PathVariable songId: String): ResponseEntity<Song> {
        val song = songService.getSong(songId)
        return if (song != null) {
            ResponseEntity.ok(song)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createSong(@RequestBody request: RegisterSongRequest): ResponseEntity<Void> {
        // RegisterSongRequestからSongへの変換は別途実装が必要
        // 現時点ではsaveSongメソッドを呼び出す
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{songId}")
    fun deleteSong(@PathVariable songId: String): ResponseEntity<Void> {
        songService.deleteSong(songId)
        return ResponseEntity.noContent().build()
    }
}