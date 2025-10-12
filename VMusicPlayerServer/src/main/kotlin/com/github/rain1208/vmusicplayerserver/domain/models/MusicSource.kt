package com.github.rain1208.vmusicplayerserver.domain.models

data class MusicSource(
    val sourceId: String,
    val title: String,
    val url: String,
    val uploadDate: String,
    val thumbnailUrl: String,
    val sourceType: MusicSourceType,
)