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
  WHERE s.title ILIKE '%' || :song_title || '%'
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
            "song_title" to songTitle,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun searchBySongArtist(songArtist: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT DISTINCT ss.song_id
  FROM song_singers ss
  INNER JOIN singers si ON ss.singer_id = si.id
  WHERE si.name ILIKE '%' || :song_artist || '%'  -- 部分一致で検索
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
            "song_artist" to songArtist,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun searchBySingerName(singerName: String): List<Song> {
        val sql = """
WITH target_singer AS (
  SELECT id
  FROM singers
  WHERE name ILIKE '%' || :singer_name || '%'
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
            "singer_name" to singerName,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun getSongsByMusicSourceId(musicSourceId: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT s.id AS song_id
  FROM songs s
  INNER JOIN music_sources ms ON s.source_id = ms.id
  WHERE ms.public_id = :music_source_id
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
            "music_source_id" to musicSourceId
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun getSongsBySingerId(singerId: String): List<Song> {
        val sql = """
WITH target_singer AS (
  SELECT id
  FROM singers
  WHERE public_id = :singer_id
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
            "singer_id" to singerId,
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun generalSearch(query: String): List<Song> {
        val sql = """
WITH target_songs AS (
  SELECT DISTINCT s.id AS song_id
  FROM songs s
  LEFT JOIN song_singers ss ON s.id = ss.song_id
  LEFT JOIN singers si ON ss.singer_id = si.id
  WHERE s.title ILIKE '%' || :query || '%'
     OR s.artist ILIKE '%' || :query || '%'
     OR si.name ILIKE '%' || :query || '%'
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
            "query" to query
        )

        return namedJdbc.query(sql, params, songRowMapper)
    }

    override fun complexSearch(title: String?, singers: List<String>, artist: String?): List<Song> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (title != null) {
            conditions.add("s.title ILIKE '%' || :title || '%'")
            params["title"] = title
        }

        if (artist != null) {
            conditions.add("s.artist ILIKE '%' || :artist || '%'")
            params["artist"] = artist
        }

        // 複数の歌手を指定する場合、すべての歌手を含む曲を検索
        if (singers.isNotEmpty()) {
            singers.forEachIndexed { index, singerName ->
                val paramName = "singer$index"
                conditions.add("""
                    s.id IN (
                        SELECT ss.song_id 
                        FROM song_singers ss 
                        INNER JOIN singers si ON ss.singer_id = si.id 
                        WHERE si.name ILIKE '%' || :$paramName || '%'
                    )
                """.trimIndent())
                params[paramName] = singerName
            }
        }

        if (conditions.isEmpty()) {
            return emptyList()
        }

        val whereClause = conditions.joinToString(" AND ")

        val sql = """
WITH target_songs AS (
  SELECT DISTINCT s.id AS song_id
  FROM songs s
  WHERE $whereClause
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

        return namedJdbc.query(sql, params, songRowMapper)
    }
}