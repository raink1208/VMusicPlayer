package com.github.rain1208.vmusicplayerserver.repository.mapper

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.repository.dto.MusicSourceDto
import com.github.rain1208.vmusicplayerserver.repository.dto.SingerDto
import com.github.rain1208.vmusicplayerserver.repository.extension.getDuration
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SongRowMapper: RowMapper<Song> {
    private val objectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    override fun mapRow(rs: ResultSet, rowNum: Int): Song {
        val singersJson = rs.getString("singers")
        val singers: List<SingerDto> = objectMapper.readValue(singersJson)

        return Song(
            songId = rs.getString("song_id"),
            title = rs.getString("song_title"),
            artist = rs.getString("song_artist"),
            startAt = rs.getDuration("song_start_at"),
            endAt = rs.getDuration("song_end_at"),
            source = MusicSourceDto(
                sourceId = rs.getString("music_source_id"),
                title = rs.getString("music_source_title"),
                url = rs.getString("music_source_url"),
                uploadDate = rs.getString("music_source_upload_date"),
                thumbnailUrl = rs.getString("music_source_thumbnail_url"),
                sourceType = rs.getString("music_source_type")
            ),
            singers = singers
        )
    }
}