package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.models.MusicSource
import com.github.rain1208.vmusicplayerserver.domain.repository.IMusicSourceRepository
import com.github.rain1208.vmusicplayerserver.repository.mapper.MusicSourceRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MusicSourceRepository(private val namedJdbc: NamedParameterJdbcTemplate): IMusicSourceRepository {
    private val musicSourceRowMapper = MusicSourceRowMapper()

    override fun getAllMusicSources(): List<MusicSource> {
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

    override fun saveMusicSource(musicSource: MusicSource): Boolean {
        val sql = """
INSERT INTO music_sources(public_id, title, url, upload_date, type_id, thumbnail_url)
VALUES (:public_id, :title, :url, :upload_date, :type_id, :thumbnail_url)
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
            "public_id" to musicSource.sourceId,
            "title" to musicSource.title,
            "url" to musicSource.url,
            "upload_date" to musicSource.uploadDate,
            "type_id" to musicSource.sourceType,
            "thumbnail_url" to musicSource.thumbnailUrl,
        )

        namedJdbc.update(sql, params)
        return true
    }

    override fun deleteMusicSource(musicSourceId: String): Boolean {
        val sql = """DELETE FROM music_sources WHERE public_id = :music_source_id;"""

        val params = mapOf(
            "music_source_id" to musicSourceId
        )

        namedJdbc.update(sql, params)
        return true
    }
}