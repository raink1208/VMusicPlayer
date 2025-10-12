package com.github.rain1208.vmusicplayerserver.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MusicSourceRepositoryTest {

    @Autowired
    private lateinit var musicSourceRepository: MusicSourceRepository

    @Test
    fun getAllMusicSource() {
        for (source in musicSourceRepository.getAllMusicSources()) {
            println(source)
        }
    }

}