package com.github.rain1208.vmusicplayerserver.services

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.repository.SongRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SongService(private val songRepository: SongRepository) {
    private val logger = LoggerFactory.getLogger(SongService::class.java)

    fun getAllSongs(): List<Song> {
        logger.info("Fetching all songs")
        val songs = songRepository.getAllSongs()
        logger.info("Retrieved {} songs", songs.size)
        return songs
    }

    fun getSong(songId: String): Song? {
        logger.info("Fetching song: songId={}", songId)
        val song = songRepository.getSong(songId)
        if (song != null) {
            logger.info("Song found: songId={}, title={}", songId, song.title)
        } else {
            logger.warn("Song not found: songId={}", songId)
        }
        return song
    }

    fun saveSong(song: Song) {
        logger.info("Saving song: songId={}, title={}", song.songId, song.title)
        songRepository.saveSong(song)
        logger.info("Song saved successfully: songId={}", song.songId)
    }

    fun deleteSong(songId: String) {
        logger.info("Deleting song: songId={}", songId)
        songRepository.deleteSong(songId)
        logger.info("Song deleted successfully: songId={}", songId)
    }
}