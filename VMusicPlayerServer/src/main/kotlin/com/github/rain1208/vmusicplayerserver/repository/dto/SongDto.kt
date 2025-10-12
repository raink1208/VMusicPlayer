package com.github.rain1208.vmusicplayerserver.repository.dto

import java.time.Duration

data class SongDto(
    val songId: String,
    val title: String,
    val artist: String?,
    val startAt: Duration,
    val endAt: Duration,
    val sourceId: String,
    val singerIds: List<String>,
)