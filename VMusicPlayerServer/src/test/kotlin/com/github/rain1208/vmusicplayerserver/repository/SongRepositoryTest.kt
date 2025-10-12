package com.github.rain1208.vmusicplayerserver.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SongRepositoryTest {

    @Autowired
    private lateinit var songRepository: SongRepository

    @Test
    fun getAllSong() {
        for (song in songRepository.getAllSongs()) {
            println(song)
        }
    }

    @Test
    fun getSongsByMusicSourceId() {
    }

    @Test
    fun getSongsBySinger() {
    }

    @Test
    fun getSongById() {
        println(songRepository.getSong("01K6SHM68FJYNYQJV8876CXNRE"))
    }

}