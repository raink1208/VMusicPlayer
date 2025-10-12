package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.domain.repository.ISearchRepository
import com.github.rain1208.vmusicplayerserver.repository.mapper.SongRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SearchRepository(private val namedJdbc: NamedParameterJdbcTemplate): ISearchRepository {
    private val songRowMapper = SongRowMapper()

    override fun searchBySongTitle(songTitle: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT s.id AS song_id
  FROM songs s
  WHERE s.title ILIKE '%' || :songTitle || '%'
),
song_singers_agg AS (
  SELECT
    ss.song_id,
    STRING_AGG(si.name, ', ') AS singers
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
  ssa.singers
FROM target_songs ts
INNER JOIN songs s ON ts.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
INNER JOIN song_singers_agg ssa ON s.id = ssa.song_id;
"""

        val params = mapOf(
            "songTitle" to songTitle,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun searchBySongArtist(songArtist: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT DISTINCT ss.song_id
  FROM song_singers ss
  INNER JOIN singers si ON ss.singer_id = si.id
  WHERE si.name ILIKE '%' || :songArtist || '%'  -- 部分一致で検索
),
song_singers_agg AS (
  SELECT
    ss.song_id,
    STRING_AGG(si.name, ', ') AS singers
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
  ssa.singers
FROM target_songs ts
INNER JOIN songs s ON ts.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
INNER JOIN song_singers_agg ssa ON s.id = ssa.song_id;
"""

        val params = mapOf(
            "songArtist" to songArtist,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun searchBySingerName(singerName: String): List<Song> {
        val sql = """
WITH target_singer AS (
  SELECT id
  FROM singers
  WHERE name ILIKE '%' || :singerName || '%'
),
target_songs AS (
  SELECT DISTINCT ss.song_id
  FROM song_singers ss
  WHERE ss.singer_id IN (SELECT id FROM target_singer)
),
song_singers_agg AS (
  SELECT 
    ss.song_id,
    json_agg(
      json_build_object(
        'singer_id', si.public_id,
        'singer_name', si.name
      ) ORDER BY si.name
    ) AS singers
  FROM song_singers ss
  INNER JOIN singers si ON ss.singer_id = si.id
  WHERE ss.song_id IN (SELECT song_id FROM target_songs)
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
  ssa.singers
FROM target_songs ts
INNER JOIN songs s ON ts.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
INNER JOIN song_singers_agg ssa ON s.id = ssa.song_id;
"""

        val params = mapOf(
            "singerName" to singerName,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun getSongsByMusicSourceId(musicSourceId: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT s.id AS song_id
  FROM songs s
  INNER JOIN music_sources ms ON s.source_id = ms.id
  WHERE ms.public_id = :musicSourceId
),
song_singers_agg AS (
  SELECT
    ss.song_id,
    STRING_AGG(si.name, ', ') AS singers
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
  ssa.singers
FROM target_songs ts
INNER JOIN songs s ON ts.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
LEFT JOIN song_singers_agg ssa ON s.id = ssa.song_id;
"""

        val params = mapOf(
            "musicSourceId" to musicSourceId
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun getSongsBySingerId(singerId: String): List<Song> {
        val sql = """
WITH target_singer AS (
  SELECT id
  FROM singers
  WHERE public_id = :singerId
),
target_songs AS (
  SELECT DISTINCT ss.song_id
  FROM song_singers ss
  WHERE ss.singer_id IN (SELECT id FROM target_singer)
),
song_singers_agg AS (
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
  WHERE ss.song_id IN (SELECT song_id FROM target_songs)
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
  ssa.singers
FROM target_songs ts
INNER JOIN songs s ON ts.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
INNER JOIN song_singers_agg ssa ON s.id = ssa.song_id;
"""

        val params = mapOf(
            "singerId" to singerId,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }
}