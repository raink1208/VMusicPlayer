package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.MusicSource

interface IMusicSourceRepository {
    fun getAllMusicSources(): List<MusicSource>
    fun saveMusicSource(musicSource: MusicSource): Boolean
    fun deleteMusicSource(musicSourceId: String): Boolean
}