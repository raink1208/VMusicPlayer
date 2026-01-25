package com.github.rain1208.vmusicplayerserver.controller

import com.github.rain1208.vmusicplayerserver.domain.models.Song
import com.github.rain1208.vmusicplayerserver.services.SearchService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/search")
class SearchController(private val searchService: SearchService) {
    private val logger = LoggerFactory.getLogger(SearchController::class.java)

    @GetMapping
    fun search(@RequestParam("q") query: String): List<Song> {
        logger.info("Search request received: {}", query)
        return searchService.unifiedSearch(query)
    }

    @GetMapping("/title")
    fun searchByTitle(@RequestParam("q") title: String): List<Song> {
        logger.info("Search by title request received: {}", title)
        return searchService.searchBySongTitle(title)
    }

    @GetMapping("/artist")
    fun searchByArtist(@RequestParam("q") artist: String): List<Song> {
        logger.info("Search by artist request received: {}", artist)
        return searchService.searchBySongArtist(artist)
    }

    @GetMapping("/singer")
    fun searchBySinger(@RequestParam("q") singer: String): List<Song> {
        logger.info("Search by singer request received: {}", singer)
        return searchService.searchBySingerName(singer)
    }
}