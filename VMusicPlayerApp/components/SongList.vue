<template>
  <div class="song-list-container">
    <div class="main-layout">
      <!-- 左側: 動画プレイヤー (40%) -->
      <div class="video-section">
        <div class="video-zone">
          <div v-if="selectedSong" class="video-player">
            <YouTubePlayer
              :video-id="extractYouTubeVideoId(selectedSong.source.url)"
              :start-time="durationToSeconds(selectedSong.startAt)"
              :end-time="durationToSeconds(selectedSong.endAt)"
              :player-id="selectedSong.songId"
            />
          </div>
          <div v-else class="video-placeholder">
            <div class="placeholder-content">
              <span class="placeholder-icon">🎵</span>
              <p>楽曲を選択してください</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右側: プレイリスト (60%) -->
      <div class="playlist-section">
        <!-- 検索セクション -->
        <div class="search-section">
          <div class="search-input-container">
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="検索..."
              @keyup.enter="performSearch"
            />
            <button @click="performSearch" class="search-button">
              🔍
            </button>
            <button v-if="isSearchActive" @click="clearSearch" class="clear-button">
              ✕
            </button>
          </div>
        </div>

        <div v-if="pending" class="loading">
          読み込み中...
        </div>

        <div v-else-if="error" class="error">
          <p>エラーが発生しました: {{ error.message }}</p>
          <button @click="() => refresh()">再読み込み</button>
        </div>

        <div v-else-if="displayedSongs && displayedSongs.length > 0" class="song-list">
          <div
            v-for="song in displayedSongs"
            :key="song.songId"
            class="song-list-item"
            :class="{
              'selected': selectedSong?.songId === song.songId,
              'clickable': song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'
            }"
            @click="selectSong(song)"
          >
            <div class="item-thumbnail">
              <img :src="song.source.thumbnailUrl" :alt="song.title" />
              <div
                v-if="song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'"
                class="play-overlay"
              >
                <span class="play-icon">▶</span>
              </div>
            </div>
            <div class="item-content">
              <div class="item-title">{{ song.title }}</div>
              <div class="item-artist" v-if="song.artist">{{ song.artist }}</div>
              <div class="item-singers">
                <span v-for="singer in song.singers" :key="singer.singerId" class="singer-tag">
                  {{ singer.singerName }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="no-songs">
          <p v-if="isSearchActive">検索結果が見つかりませんでした</p>
          <p v-else>楽曲が登録されていません</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Song } from '../types/song'
import YouTubePlayer from './YouTubePlayer.vue'
import { ref, computed, watch } from 'vue'
import { useRuntimeConfig, useFetch } from 'nuxt/app'
import { $fetch } from 'ofetch'

const config = useRuntimeConfig()

// 選択された曲の状態
const selectedSong = ref<Song | null>(null)

// 検索関連の状態
const searchQuery = ref<string>('')
const isSearchActive = ref<boolean>(false)
const searchResults = ref<Song[] | null>(null)

// 曲を選択する関数
const selectSong = (song: Song) => {
  // LIVE または VIDEO のみ選択可能
  if (song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO') {
    selectedSong.value = song
    console.log('選択された曲:', song.title)
  }
}

// 検索を実行する関数
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    clearSearch()
    return
  }

  try {
    const response = await $fetch<Song[]>(
      `${config.public.apiBase}/api/search?q=${encodeURIComponent(searchQuery.value.trim())}`
    )
    searchResults.value = response
    isSearchActive.value = true
    console.log('検索結果:', response.length, '件')
  } catch (err) {
    console.error('検索エラー:', err)
    searchResults.value = []
  }
}

// 検索をクリアする関数
const clearSearch = () => {
  searchQuery.value = ''
  searchResults.value = null
  isSearchActive.value = false
}

// YouTube URLから動画IDを抽出
const extractYouTubeVideoId = (url: string): string => {
  if (!url) return ''

  // https://www.youtube.com/watch?v=VIDEO_ID 形式
  const watchMatch = url.match(/[?&]v=([^&]+)/)
  if (watchMatch && watchMatch[1]) return watchMatch[1]

  // https://youtu.be/VIDEO_ID 形式
  const shortMatch = url.match(/youtu\.be\/([^?]+)/)
  if (shortMatch && shortMatch[1]) return shortMatch[1]

  // https://www.youtube.com/live/VIDEO_ID 形式
  const liveMatch = url.match(/youtube\.com\/live\/([^?]+)/)
  if (liveMatch && liveMatch[1]) return liveMatch[1]

  return ''
}

// APIから曲一覧を取得
const { data: songs, pending, error, refresh } = await useFetch<Song[]>(
  `${config.public.apiBase}/api/songs`
)

// 表示する曲リストを計算（検索結果または全曲）
const displayedSongs = computed(() => {
  return isSearchActive.value && searchResults.value !== null
    ? searchResults.value
    : songs.value || []
})

// データを取得したらコンソールに表示
watch(songs, (newSongs: Song[] | null | undefined) => {
  if (newSongs) {
    console.log('=== 楽曲データ取得 ===')
    console.log(`楽曲数: ${newSongs.length}`)
    newSongs.forEach((song: Song, index: number) => {
      console.log(`[${index + 1}] ${song.title}`)
      console.log(`  - sourceType: ${song.source.sourceType}`)
      console.log(`  - URL: ${song.source.url}`)
      console.log(`  - VideoID: ${extractYouTubeVideoId(song.source.url)}`)
      console.log(`  - 再生範囲: ${song.startAt} - ${song.endAt}`)
    })
  }
}, { immediate: true })

// ...existing code...
// Duration文字列をフォーマット (ISO 8601形式: PT1M30Sなど)
const formatDuration = (duration: string): string => {
  if (!duration) return '0:00'

  // PT1M30S -> 1:30 のような形式に変換
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration

  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = Math.floor(parseFloat(match[3] || '0'))

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  }
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

// Duration文字列を秒数に変換
const durationToSeconds = (duration: string): number => {
  if (!duration) return 0

  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return 0

  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = Math.floor(parseFloat(match[3] || '0'))

  return hours * 3600 + minutes * 60 + seconds
}

// YouTube URLに再生開始時刻を追加
const getYoutubeUrlWithTimestamp = (url: string, startAt: string): string => {
  if (!url || !startAt) return url

  const seconds = durationToSeconds(startAt)
  const separator = url.includes('?') ? '&' : '?'

  return `${url}${separator}t=${seconds}`
}
</script>

<style scoped>
.song-list-container {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
  background: #f5f5f5;
}

.main-layout {
  display: flex;
  width: 100%;
  height: 100%;
  gap: 0;
}

/* 左側: 動画セクション (40%) */
.video-section {
  width: 40%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ddd;
  padding: 16px;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
}

.video-zone {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  position: relative;
}

.video-player {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #000;
  position: relative;
}

.close-button {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(244, 67, 54, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  font-size: 1.3rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.close-button:hover {
  background: #d32f2f;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.placeholder-content {
  text-align: center;
  color: white;
}

.placeholder-icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 20px;
}

.placeholder-content p {
  font-size: 1.3rem;
  margin: 0;
  font-weight: 300;
}

/* 右側: プレイリストセクション (60%) */
.playlist-section {
  width: 60%;
  height: 100%;
  background: white;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.playlist-header {
  padding: 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.playlist-header h1 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

/* 検索セクション */
.search-section {
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.search-input-container {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 1;
  padding: 10px 12px;
  font-size: 0.95rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
}

.search-button {
  padding: 10px 14px;
  font-size: 0.95rem;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.search-button:hover {
  background-color: #1565c0;
}

.clear-button {
  padding: 10px 12px;
  font-size: 0.95rem;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.clear-button:hover {
  background-color: #d32f2f;
}

/* プレイリスト表示 */
.song-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.song-list-item {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  align-items: flex-start;
  transition: background-color 0.2s, border-left 0.2s;
  border-left: 3px solid transparent;
}

.song-list-item.clickable {
  cursor: pointer;
}

.song-list-item.clickable:hover {
  background-color: #f9f9f9;
}

.song-list-item.selected {
  background-color: #e3f2fd;
  border-left-color: #1976d2;
}

.item-thumbnail {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  background: #f0f0f0;
  position: relative;
}

.item-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.song-list-item.clickable:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 1.5rem;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-weight: 600;
  font-size: 0.95rem;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-artist {
  font-size: 0.85rem;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-singers {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.singer-tag {
  display: inline-block;
  padding: 2px 8px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 10px;
  font-size: 0.75rem;
  white-space: nowrap;
}

/* ローディング・エラー・空状態 */
.loading,
.error,
.no-songs {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  text-align: center;
  padding: 40px 20px;
  font-size: 1rem;
  color: #666;
}

.error {
  color: #d32f2f;
  flex-direction: column;
}

.error button {
  margin-top: 15px;
  padding: 8px 16px;
  font-size: 0.95rem;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.error button:hover {
  background-color: #1565c0;
}

/* レスポンシブ調整 */
@media (max-width: 1024px) {
  .video-section {
    width: 45%;
  }

  .playlist-section {
    width: 55%;
  }
}

@media (max-width: 768px) {
  .main-layout {
    flex-direction: column;
  }

  .video-section {
    width: 100%;
    height: 50vh;
  }

  .playlist-section {
    width: 100%;
    height: 50vh;
  }

  .item-thumbnail {
    width: 50px;
    height: 50px;
  }

  .song-list-item {
    gap: 10px;
    padding: 8px 10px;
  }

  .item-title {
    font-size: 0.9rem;
  }

  .item-artist {
    font-size: 0.8rem;
  }

  .singer-tag {
    font-size: 0.7rem;
    padding: 2px 6px;
  }
}
</style>
