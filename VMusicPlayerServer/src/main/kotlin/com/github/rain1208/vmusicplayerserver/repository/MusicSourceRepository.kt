package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.repository.IMusicSourceRepository
import com.github.rain1208.vmusicplayerserver.repository.dto.MusicSourceDto
import com.github.rain1208.vmusicplayerserver.repository.mapper.MusicSourceRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MusicSourceRepository(private val namedJdbc: NamedParameterJdbcTemplate): IMusicSourceRepository {
    private val musicSourceRowMapper = MusicSourceRowMapper()

    override fun getAllMusicSources(): List<MusicSourceDto> {
        val sql = """
SELECT
  ms.public_id AS music_source_id,
  ms.title AS music_source_title,
  ms.url AS music_source_url,
  ms.upload_date AS music_source_upload_date,
  ms.thumbnail_url AS music_source_thumbnail_url,
  mst.name AS music_source_type
FROM music_sources ms
JOIN music_source_types mst ON ms.type_id = mst.id;
"""
        return namedJdbc.query(sql, musicSourceRowMapper)
    }

    override fun saveMusicSource(musicSourceDto: MusicSourceDto): Boolean {
        val sql = """
INSERT INTO music_sources(public_id, title, url, upload_date, type_id, thumbnail_url)
VALUES (:public_id, :title, :url, :upload_date, :type, :thumbnail_url)
ON CONFLICT (public_id)
DO UPDATE SET
    public_id = EXCLUDED.public_id,
    title = EXCLUDED.title,
    url = EXCLUDED.url,
    upload_date = EXCLUDED.upload_date,
    type_id = EXCLUDED.type_id,
    updated_at = CURRENT_TIMESTAMP
"""

        val params = mapOf(
            "public_id" to musicSourceDto.sourceId,
            "title" to musicSourceDto.title,
            "url" to musicSourceDto.url,
            "upload_date" to musicSourceDto.uploadDate,
            "type" to musicSourceDto.sourceType,
            "thumbnail_url" to musicSourceDto.thumbnailUrl,
        )

        namedJdbc.update(sql, params)
        return true
    }

    override fun deleteMusicSource(musicSourceId: String): Boolean {
        val sql = """DELETE FROM music_sources WHERE public_id = :musicSourceId;"""

        val params = mapOf(
            "musicSourceId" to musicSourceId
        )

        namedJdbc.update(sql, params)
        return true
    }
}