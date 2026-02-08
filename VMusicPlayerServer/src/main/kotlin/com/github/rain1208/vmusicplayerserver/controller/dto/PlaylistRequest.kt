package com.github.rain1208.vmusicplayerserver.controller.dto

data class PlaylistCreateRequest(
    val name: String,
    val songIds: List<String>
)

data class PlaylistInfoUpdateRequest(
    val name: String
)

data class PlaylistSongsUpdateRequest(
    val songIds: List<String>
)