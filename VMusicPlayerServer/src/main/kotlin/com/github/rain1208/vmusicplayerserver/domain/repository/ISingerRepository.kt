package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Singer

interface ISingerRepository {
    fun getAllSingers(): List<Singer>
    fun saveSinger(singer: Singer): Boolean
    fun deleteSinger(singerId: String): Boolean
}