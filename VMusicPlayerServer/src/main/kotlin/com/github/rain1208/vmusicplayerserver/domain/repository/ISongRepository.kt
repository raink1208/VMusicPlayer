package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Song

interface ISongRepository {
    fun getAllSongs(): List<Song>
    fun getSong(songId: String): Song?
    fun saveSong(song: Song): Boolean
    fun deleteSong(songId: String): Boolean
}