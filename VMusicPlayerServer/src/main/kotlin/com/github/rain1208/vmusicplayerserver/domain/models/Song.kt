package com.github.rain1208.vmusicplayerserver.domain.models

import com.github.rain1208.vmusicplayerserver.repository.dto.MusicSourceDto
import com.github.rain1208.vmusicplayerserver.repository.dto.SingerDto
import com.github.rain1208.vmusicplayerserver.repository.dto.SongDto
import java.time.Duration

data class Song(
    val songId: String,
    val title: String,
    val artist: String?,
    val startAt: Duration,
    val endAt: Duration,
    val source: MusicSourceDto,
    val singers: List<SingerDto>,
) {
    fun toSongDto(): SongDto {
        return SongDto(
            songId = songId,
            title = title,
            artist = artist,
            startAt = startAt,
            endAt = endAt,
            sourceId = source.sourceId,
            singerIds = singers.map { it.singerId }
        )
    }
}