package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Song

interface ISearchRepository {
    fun searchBySongTitle(songTitle: String): List<Song>
    fun searchBySongArtist(songArtist: String): List<Song>

    fun searchBySingerName(singerName: String): List<Song>

    fun getSongsByMusicSourceId(musicSourceId: String): List<Song>
    fun getSongsBySingerId(singerId: String): List<Song>

    fun generalSearch(query: String): List<Song>
    fun complexSearch(title: String?, singers: List<String>, artist: String?): List<Song>
}