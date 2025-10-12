package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.repository.dto.MusicSourceDto

interface IMusicSourceRepository {
    fun getAllMusicSources(): List<MusicSourceDto>
    fun saveMusicSource(musicSourceDto: MusicSourceDto): Boolean
    fun deleteMusicSource(musicSourceId: String): Boolean
}