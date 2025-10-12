package com.github.rain1208.vmusicplayerserver.repository.mapper

import com.github.rain1208.vmusicplayerserver.repository.dto.SingerDto
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SingerRowMapper: RowMapper<SingerDto> {
    override fun mapRow(rs: ResultSet, rowNum: Int): SingerDto {
        return SingerDto(
            singerId = rs.getString("public_id"),
            singerName = rs.getString("name"),
            youtubeURL = rs.getString("youtube_url"),
            twitterURL = rs.getString("twitter_url"),
        )
    }
}