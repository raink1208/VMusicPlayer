package com.github.rain1208.vmusicplayerserver.domain.models

import java.time.LocalDateTime

data class Playlist(
    val playlistId: String,
    val name: String,
    val songCount: Int = 0,
    val firstSongThumbnail: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
