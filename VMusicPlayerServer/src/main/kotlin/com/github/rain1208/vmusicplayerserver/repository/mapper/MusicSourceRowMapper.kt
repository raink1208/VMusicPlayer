package com.github.rain1208.vmusicplayerserver.repository.mapper

import com.github.rain1208.vmusicplayerserver.repository.dto.MusicSourceDto
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class MusicSourceRowMapper: RowMapper<MusicSourceDto> {
    override fun mapRow(rs: ResultSet, rowNum: Int): MusicSourceDto {
        return MusicSourceDto(
            sourceId = rs.getString("music_source_id"),
            title = rs.getString("music_source_title"),
            url = rs.getString("music_source_url"),
            uploadDate = rs.getString("music_source_upload_date"),
            thumbnailUrl = rs.getString("music_source_thumbnail_url"),
            sourceType = rs.getString("music_source_type")
        )
    }
}