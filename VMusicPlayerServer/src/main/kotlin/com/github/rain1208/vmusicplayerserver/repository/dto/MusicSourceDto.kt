package com.github.rain1208.vmusicplayerserver.repository.dto

data class MusicSourceDto(
    val sourceId: String,
    val title: String,
    val url: String,
    val uploadDate: String,
    val thumbnailUrl: String,
    val sourceType: String,
)
