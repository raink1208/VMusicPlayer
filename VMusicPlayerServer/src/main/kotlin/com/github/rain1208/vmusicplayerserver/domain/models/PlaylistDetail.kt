package com.github.rain1208.vmusicplayerserver.domain.models

import java.time.LocalDateTime

data class PlaylistDetail(
    val playlistId: String,
    val name: String,
    val songs: List<PlaylistSongItem>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class PlaylistSongItem(
    val position: Int,
    val song: Song
)

