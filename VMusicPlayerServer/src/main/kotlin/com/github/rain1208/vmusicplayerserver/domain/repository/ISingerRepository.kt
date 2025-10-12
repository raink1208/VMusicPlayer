package com.github.rain1208.vmusicplayerserver.domain.repository

import com.github.rain1208.vmusicplayerserver.repository.dto.SingerDto

interface ISingerRepository {
    fun getAllSingers(): List<SingerDto>
    fun saveSinger(singer: SingerDto): Boolean
    fun deleteSinger(singerId: String): Boolean
}