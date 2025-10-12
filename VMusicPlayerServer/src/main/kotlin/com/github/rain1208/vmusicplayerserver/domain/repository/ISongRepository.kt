package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.repository.dto.SongDto

interface ISongRepository {
    fun getAllSongs(): List<Song>
    fun getSong(songId: String): Song?
    fun saveSong(song: SongDto): Boolean
    fun deleteSong(songId: String): Boolean
}