package com.github.rain1208.vmusicplayerserver.repository.mapper

import com.github.rain1208.vmusicplayerserver.domain.models.Singer
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SingerRowMapper: RowMapper<Singer> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Singer {
        return Singer(
            singerId = rs.getString("public_id"),
            singerName = rs.getString("name"),
            youtubeURL = rs.getString("youtube_url"),
            twitterURL = rs.getString("twitter_url"),
        )
    }
}