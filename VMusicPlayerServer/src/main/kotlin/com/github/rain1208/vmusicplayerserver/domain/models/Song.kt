package com.github.rain1208.vmusicplayerserver.domain.models

import java.time.Duration

data class Song(
    val songId: String,
    val title: String,
    val artist: String?,
    val startAt: Duration,
    val endAt: Duration,
    val source: MusicSource,
    val singers: List<Singer>,
)