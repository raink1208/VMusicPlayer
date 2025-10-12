package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.domain.repository.ISongRepository
import com.github.rain1208.vmusicplayerserver.repository.dto.SongDto
import com.github.rain1208.vmusicplayerserver.repository.mapper.SongRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SongRepository(private val namedJdbc: NamedParameterJdbcTemplate): ISongRepository {

    private val songRowMapper = SongRowMapper()

    override fun getAllSongs(): List<Song> {
        val sql = """
WITH song_singers_agg AS (
  SELECT 
    ss.song_id,
    json_agg(
      json_build_object(
        'singer_id', si.public_id,
        'singer_name', si.name,
        'youtube_url', si.youtube_url,
        'twitter_url', si.twitter_url
      ) ORDER BY si.name
    ) AS singers
  FROM song_singers ss
  INNER JOIN singers si ON ss.singer_id = si.id
  GROUP BY ss.song_id
)
SELECT
  s.public_id AS song_id,
  s.title AS song_title,
  s.artist as song_artist,
  s.start_at AS song_start_at,
  s.end_at AS song_end_at,
  ms.public_id AS music_source_id,
  ms.title AS music_source_title,
  ms.url AS music_source_url,
  ms.upload_date AS music_source_upload_date,
  ms.thumbnail_url AS music_source_thumbnail_url,
  mst.name AS music_source_type,
  COALESCE(ssa.singers, '[]'::json) AS singers
FROM songs s
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
LEFT JOIN song_singers_agg ssa ON s.id = ssa.song_id
ORDER BY s.created_at DESC;
"""
        return namedJdbc.query(sql, songRowMapper)
    }

    override fun getSong(songId: String): Song? {
        val sql = """
WITH song_singers_agg AS (
  SELECT 
    ss.song_id,
    json_agg(
      json_build_object(
        'singer_id', si.public_id,
        'singer_name', si.name,
        'youtube_url', si.youtube_url,
        'twitter_url', si.twitter_url
      ) ORDER BY si.name
    ) AS singers
  FROM song_singers ss
  INNER JOIN singers si ON ss.singer_id = si.id
  INNER JOIN songs s ON ss.song_id = s.id
  WHERE s.public_id = :songId
  GROUP BY ss.song_id
)
SELECT
  s.public_id AS song_id,
  s.title AS song_title,
  s.artist as song_artist,
  s.start_at AS song_start_at,
  s.end_at AS song_end_at,
  ms.public_id AS music_source_id,
  ms.title AS music_source_title,
  ms.url AS music_source_url,
  ms.upload_date AS music_source_upload_date,
  ms.thumbnail_url AS music_source_thumbnail_url,
  mst.name AS music_source_type,
  COALESCE(ssa.singers, '[]'::json) AS singers
FROM songs s
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
LEFT JOIN song_singers_agg ssa ON s.id = ssa.song_id
WHERE s.public_id = :songId;
"""

        val params = mapOf(
            "songId" to songId,
        )

        return namedJdbc.queryForObject(sql, params, songRowMapper)
    }

    override fun saveSong(song: SongDto): Boolean {
        val saveSongSql = """
WITH upsert_song AS (
  INSERT INTO songs (public_id, source_id, title, artist, start_at, end_at)
  VALUES (:song_public_id, :source_id, :title, :artist, :startAt, :endAt)
  ON CONFLICT (public_id)
  DO UPDATE SET
    source_id  = EXCLUDED.source_id,
    title      = EXCLUDED.title,
    artist     = EXCLUDED.artist,
    start_at   = EXCLUDED.start_at,
    end_at     = EXCLUDED.end_at,
    updated_at = CURRENT_TIMESTAMP
  RETURNING id
),
desired AS (
  SELECT us.id AS song_id, s.id AS singer_id
  FROM upsert_song us
  JOIN UNNEST(COALESCE(:singer_public_ids::text[], '{}'::text[])) spid ON TRUE
  JOIN singers s ON s.public_id = spid
),
ins AS (
  INSERT INTO song_singers (song_id, singer_id)
  SELECT song_id, singer_id FROM desired
  ON CONFLICT DO NOTHING
  RETURNING 1
),
del AS (
  DELETE FROM song_singers ss
  USING upsert_song us
  WHERE ss.song_id = us.id
    AND NOT EXISTS (SELECT 1 FROM desired d WHERE d.singer_id = ss.singer_id)
  RETURNING 1
)
SELECT (SELECT COUNT(*) FROM ins) AS inserted_count,
       (SELECT COUNT(*) FROM del) AS deleted_count,
       (SELECT id FROM upsert_song) AS song_id;
"""

        val params = mapOf(
            "song_public_id" to song.songId,
            "source_id" to song.sourceId,
            "title" to song.title,
            "artist" to song.artist,
            "start_at" to song.startAt,
            "end_at" to song.endAt,
            "singer_public_ids" to song.singerIds
        )

        namedJdbc.update(saveSongSql, params)
        return true
    }

    override fun deleteSong(songId: String): Boolean {
        val sql = """DELETE FROM songs WHERE public_id = :songId;"""

        val params = mapOf(
            "song_id" to songId
        )

        namedJdbc.update(sql, params)
        return true
    }
}