package com.github.rain1208.vmusicplayerserver.repository.mapper

import com.github.rain1208.vmusicplayerserver.domain.models.MusicSource
import com.github.rain1208.vmusicplayerserver.domain.models.MusicSourceType
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class MusicSourceRowMapper: RowMapper<MusicSource> {
    override fun mapRow(rs: ResultSet, rowNum: Int): MusicSource {
        return MusicSource(
            sourceId = rs.getString("music_source_id"),
            title = rs.getString("music_source_title"),
            url = rs.getString("music_source_url"),
            uploadDate = rs.getString("music_source_upload_date"),
            thumbnailUrl = rs.getString("music_source_thumbnail_url"),
            sourceType = MusicSourceType.valueOf(rs.getString("music_source_type"))
        )
    }
}