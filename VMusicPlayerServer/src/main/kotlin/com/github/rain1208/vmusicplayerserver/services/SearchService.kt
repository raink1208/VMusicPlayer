package com.github.rain1208.vmusicplayerserver.services

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.domain.repository.ISearchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SearchService(private val searchRepository: ISearchRepository) {
    private val logger = LoggerFactory.getLogger(SearchService::class.java)

    fun searchBySongTitle(songTitle: String): List<Song> {
        logger.info("SearchBySongTitle called {}", songTitle)
        val songs = searchRepository.searchBySongTitle(songTitle)
        logger.info("SearchBySongTitle found {} songs", songs.size)
        return songs
    }

    fun searchBySongArtist(songArtist: String): List<Song> {
        logger.info("SearchBySongArtis called {}", songArtist)
        val songs = searchRepository.searchBySongArtist(songArtist)
        logger.info("SearchBySongArtis found {} songs", songs.size)
        return songs
    }

    fun searchByMusicSourceId(musicSourceId: String): List<Song> {
        logger.info("SearchByMusicSourceId called {}", musicSourceId)
        val songs = searchRepository.getSongsByMusicSourceId(musicSourceId)
        logger.info("SearchByMusicSourceId found {} songs", songs.size)
        return songs
    }

    fun searchBySingerId(singerId: String): List<Song> {
        logger.info("SearchBySingerId called {}", singerId)
        val songs = searchRepository.getSongsBySingerId(singerId)
        logger.info("SearchBySingerId found {} songs", songs.size)
        return songs
    }

    fun searchBySingerName(singerName: String): List<Song> {
        logger.info("SearchBySingerName called {}", singerName)
        val songs = searchRepository.searchBySingerName(singerName)
        logger.info("SearchBySingerName found {} songs", songs.size)
        return songs
    }

    fun unifiedSearch(query: String): List<Song> {
        logger.info("UnifiedSearch called with query: {}", query)

        val trimmedQuery = query.trim()

        val searchCriteria = parseSearchQuery(trimmedQuery)

        val songs = when {
            // すべての条件が空の場合
            searchCriteria.title == null && searchCriteria.singers.isEmpty() &&
            searchCriteria.artist == null && searchCriteria.general == null -> {
                emptyList()
            }
            // 通常検索（プレフィックスなし）
            searchCriteria.general != null -> {
                searchRepository.generalSearch(searchCriteria.general)
            }
            // タイトルのみの検索
            searchCriteria.title != null && searchCriteria.singers.isEmpty() && searchCriteria.artist == null -> {
                searchRepository.searchBySongTitle(searchCriteria.title)
            }
            // 歌手1人のみの検索
            searchCriteria.title == null && searchCriteria.singers.size == 1 && searchCriteria.artist == null -> {
                searchRepository.searchBySingerName(searchCriteria.singers[0])
            }
            // アーティストのみの検索
            searchCriteria.title == null && searchCriteria.singers.isEmpty() -> {
                searchRepository.searchBySongArtist(searchCriteria.artist!!)
            }
            // 複合検索（複数条件の組み合わせ）
            else -> {
                searchRepository.complexSearch(
                    searchCriteria.title,
                    searchCriteria.singers,
                    searchCriteria.artist
                )
            }
        }

        logger.info("UnifiedSearch found {} songs", songs.size)
        return songs
    }

    private fun parseSearchQuery(query: String): SearchCriteria {
        val singers = mutableListOf<String>()
        var title: String? = null
        var artist: String? = null
        var general: String? = null

        val regex = Regex("""(title|singer|artist):([^\s]+(?:\s+(?!(?:title|singer|artist):)[^\s]+)*)""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(query)

        if (matches.none()) {
            // プレフィックスが見つからない場合は通常検索
            if (query.isNotBlank()) {
                general = query
            }
        } else {
            // プレフィックス付きの検索条件を抽出
            matches.forEach { match ->
                val type = match.groupValues[1].lowercase()
                val term = match.groupValues[2].trim()
                if (term.isNotBlank()) {
                    when (type) {
                        "title" -> title = term
                        "singer" -> singers.add(term)
                        "artist" -> artist = term
                    }
                }
            }
        }

        return SearchCriteria(title, singers, artist, general)
    }

    private data class SearchCriteria(
        val title: String?,
        val singers: List<String>,
        val artist: String?,
        val general: String?
    )
}