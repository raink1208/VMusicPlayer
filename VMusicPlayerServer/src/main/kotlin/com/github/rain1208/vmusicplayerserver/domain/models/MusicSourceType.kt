package com.github.rain1208.vmusicplayerserver.domain.models

enum class MusicSourceType(val type: String) {
    VIDEO("video"),
    LIVE("live"),
    LOCAL("local");

    companion object {
        fun of(type: String): MusicSourceType {
            return MusicSourceType.entries.find { it.type == type }
                ?: throw IllegalArgumentException("Unknown MusicSourceType: $type")
        }
    }
}