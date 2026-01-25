package com.github.rain1208.vmusicplayerserver.controller.dto

data class RegisterSongRequest(
    val title: String,
    val artist: String,
    val startAt: Long,
    val endAt: Long,
    val sourceId: String,
    val singerIds: List<String>,
)
