package com.github.rain1208.vmusicplayerserver.repository

import com.github.rain1208.vmusicplayerserver.domain.models.Playlist
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistDetail
import com.github.rain1208.vmusicplayerserver.domain.models.PlaylistSongItem
import com.github.rain1208.vmusicplayerserver.domain.repository.IPlaylistRepository
import com.github.rain1208.vmusicplayerserver.repository.mapper.SongRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PlaylistRepository(private val namedJdbc: NamedParameterJdbcTemplate) : IPlaylistRepository {

    private val songRowMapper = SongRowMapper()

    override fun getAllPlaylists(): List<Playlist> {
        val sql = """
SELECT 
    p.public_id AS playlist_id,
    p.name AS playlist_name,
    p.created_at,
    p.updated_at,
    COUNT(ps.song_id) AS song_count,
    (
        SELECT ms.thumbnail_url 
        FROM playlist_songs ps2
        INNER JOIN songs s ON ps2.song_id = s.id
        INNER JOIN music_sources ms ON s.source_id = ms.id
        WHERE ps2.playlist_id = p.id
        ORDER BY ps2.position
        LIMIT 1
    ) AS first_song_thumbnail
FROM playlists p
LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id
GROUP BY p.id, p.public_id, p.name, p.created_at, p.updated_at
ORDER BY p.created_at DESC
        """.trimIndent()

        return namedJdbc.query(sql) { rs, _ ->
            Playlist(
                playlistId = rs.getString("playlist_id"),
                name = rs.getString("playlist_name"),
                songCount = rs.getInt("song_count"),
                firstSongThumbnail = rs.getString("first_song_thumbnail"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
            )
        }
    }

    override fun getPlaylist(playlistId: String): PlaylistDetail? {
        val playlistSql = """
SELECT public_id, name, created_at, updated_at
FROM playlists
WHERE public_id = :playlist_id
        """.trimIndent()

        val playlistParams = mapOf("playlist_id" to playlistId)

        val playlist = namedJdbc.query(playlistSql, playlistParams) { rs, _ ->
            mapOf(
                "playlistId" to rs.getString("public_id"),
                "name" to rs.getString("name"),
                "createdAt" to rs.getTimestamp("created_at").toLocalDateTime(),
                "updatedAt" to rs.getTimestamp("updated_at").toLocalDateTime()
            )
        }.firstOrNull() ?: return null

        val songsSql = """
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
  ps.position,
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
FROM playlist_songs ps
INNER JOIN songs s ON ps.song_id = s.id
INNER JOIN music_sources ms ON s.source_id = ms.id
INNER JOIN music_source_types mst ON ms.type_id = mst.id
LEFT JOIN song_singers_agg ssa ON s.id = ssa.song_id
INNER JOIN playlists p ON ps.playlist_id = p.id
WHERE p.public_id = :playlist_id
ORDER BY ps.position
        """.trimIndent()

        val songs = namedJdbc.query(songsSql, playlistParams) { rs, _ ->
            PlaylistSongItem(
                position = rs.getInt("position"),
                song = songRowMapper.mapRow(rs, 0)
            )
        }

        return PlaylistDetail(
            playlistId = playlist["playlistId"] as String,
            name = playlist["name"] as String,
            songs = songs,
            createdAt = playlist["createdAt"] as java.time.LocalDateTime,
            updatedAt = playlist["updatedAt"] as java.time.LocalDateTime
        )
    }

    @Transactional
    override fun createPlaylist(playlistId: String, name: String): Playlist {
        val sql = """
INSERT INTO playlists (public_id, name)
VALUES (:public_id, :name)
RETURNING public_id, name, created_at, updated_at
        """.trimIndent()

        val params = mapOf(
            "public_id" to playlistId,
            "name" to name
        )

        return namedJdbc.queryForObject(sql, params) { rs, _ ->
            Playlist(
                playlistId = rs.getString("public_id"),
                name = rs.getString("name"),
                songCount = 0,
                firstSongThumbnail = null,
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
            )
        }!!
    }

    @Transactional
    override fun deletePlaylist(playlistId: String): Boolean {
        val sql = """
DELETE FROM playlists WHERE public_id = :playlist_id
        """.trimIndent()

        val params = mapOf("playlist_id" to playlistId)
        return namedJdbc.update(sql, params) > 0
    }

    @Transactional
    override fun updatePlaylistInfo(playlistId: String, name: String): Boolean {
        val sql = """
UPDATE playlists
SET name = :name,
    updated_at = CURRENT_TIMESTAMP
WHERE public_id = :playlist_id
        """.trimIndent()

        val params = mapOf(
            "playlist_id" to playlistId,
            "name" to name
        )
        return namedJdbc.update(sql, params) > 0
    }

    @Transactional
    override fun updatePlaylistSongs(playlistId: String, songIds: List<String>): Boolean {
        // プレイリストの内部IDを取得
        val playlistInternalIdSql = """
SELECT id FROM playlists WHERE public_id = :playlist_id
        """.trimIndent()

        val playlistInternalId = namedJdbc.queryForObject(
            playlistInternalIdSql,
            mapOf("playlist_id" to playlistId)
        ) { rs, _ -> rs.getInt("id") } ?: return false

        // 既存の楽曲関連を削除
        val deleteSql = """
DELETE FROM playlist_songs WHERE playlist_id = :playlist_id
        """.trimIndent()

        namedJdbc.update(deleteSql, mapOf("playlist_id" to playlistInternalId))

        // 新しい楽曲を追加（バッチ挿入で最適化）
        if (songIds.isNotEmpty()) {
            val insertSql = """
INSERT INTO playlist_songs (playlist_id, song_id, position)
SELECT :playlist_id, s.id, :position
FROM songs s
WHERE s.public_id = :song_public_id
            """.trimIndent()

            val batchParams = songIds.mapIndexed { index, songPublicId ->
                mapOf(
                    "playlist_id" to playlistInternalId,
                    "song_public_id" to songPublicId,
                    "position" to (index + 1)
                )
            }.toTypedArray()

            namedJdbc.batchUpdate(insertSql, batchParams)
        }

        // プレイリストのupdated_atを更新
        val updatePlaylistSql = """
UPDATE playlists
SET updated_at = CURRENT_TIMESTAMP
WHERE id = :playlist_id
        """.trimIndent()

        namedJdbc.update(updatePlaylistSql, mapOf("playlist_id" to playlistInternalId))

        return true
    }
}

