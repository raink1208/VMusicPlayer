package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.repository.ISingerRepository
import com.github.rain1208.vmusicplayerserver.repository.dto.SingerDto
import com.github.rain1208.vmusicplayerserver.repository.mapper.SingerRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SingerRepository(private val namedJdbc: NamedParameterJdbcTemplate): ISingerRepository {
    private val singerRowMapper = SingerRowMapper()

    override fun getAllSingers(): List<SingerDto> {
        val sql = """SELECT public_id, name, youtube_url FROM singers;"""

        return namedJdbc.query(sql, singerRowMapper)
    }

    override fun saveSinger(singer: SingerDto): Boolean {
        val sql = """
INSERT INTO singers (public_id, name, youtube_url, twitter_url)
VALUES (:public_id, :name, :youtube_url, :twitter_url)
ON CONFLICT (public_id)
DO UPDATE SET
    name = EXCLUDED.name,
    youtube_url = EXCLUDED.youtube_url,
    twitter_url = EXCLUDED.twitter_url,
    updated_at = CURRENT_TIMESTAMP;
"""

        val params = mapOf(
            "public_id" to singer.singerId,
            "name" to singer.singerName,
            "youtube_url" to singer.youtubeURL,
            "twitter_url" to singer.twitterURL,
        )

        namedJdbc.update(sql, params)
        return true
    }

    override fun deleteSinger(singerId: String): Boolean {
        val sql = """DELETE FROM singers WHERE (public_id = :public_id);"""

        val params = mapOf(
            "public_id" to singerId,
        )

        namedJdbc.update(sql, params)
        return true
    }
}