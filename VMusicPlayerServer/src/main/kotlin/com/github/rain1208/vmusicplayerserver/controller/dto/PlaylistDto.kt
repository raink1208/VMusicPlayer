package com.github.rain1208.vmusicplayerserver.controller.dto

data class CreatePlaylistRequest(
    val name: String
)

data class UpdatePlaylistNameRequest(
    val name: String
)

data class AddSongToPlaylistRequest(
    val songId: String
)

data class ReorderPlaylistSongsRequest(
    val songIds: List<String>
)

data class CreatePlaylistFromQueueRequest(
    val name: String,
    val songIds: List<String>
)

