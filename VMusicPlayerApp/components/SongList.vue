<template>
  <div class="song-list-container">
    <!-- 固定YouTubeプレイヤーゾーン（16:9） -->
    <div class="video-zone">
      <div v-if="selectedSong" class="video-player">
        <div class="player-header">
          <div class="player-info">
            <h3>{{ selectedSong.title }}</h3>
            <p v-if="selectedSong.artist">{{ selectedSong.artist }}</p>
          </div>
          <button @click="closePlayer" class="close-button">✕</button>
        </div>
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

    <!-- 楽曲一覧セクション -->
    <div class="list-section">
      <h1>楽曲一覧</h1>

      <!-- 検索セクション -->
      <div class="search-section">
        <div class="search-input-container">
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="楽曲名、アーティスト、歌手で検索..."
            @keyup.enter="performSearch"
          />
          <button @click="performSearch" class="search-button">
            🔍 検索
          </button>
          <button v-if="isSearchActive" @click="clearSearch" class="clear-button">
            ✕ クリア
          </button>
        </div>
        <div class="search-help">
          <p>高度な検索: <code>title:曲名</code> <code>artist:作曲者</code> <code>singer:歌手名</code></p>
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
          class="song-card"
          :class="{
            'selected': selectedSong?.songId === song.songId,
            'clickable': song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'
          }"
          @click="selectSong(song)"
        >
          <div class="song-thumbnail">
            <img :src="song.source.thumbnailUrl" :alt="song.title" />
            <div
              v-if="song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'"
              class="play-overlay"
            >
              <span class="play-icon">▶</span>
            </div>
          </div>
          <div class="song-info">
            <h2 class="song-title">{{ song.title }}</h2>
            <p class="song-artist" v-if="song.artist">{{ song.artist }}</p>
            <div class="song-singers">
              <span v-for="singer in song.singers" :key="singer.singerId" class="singer-tag">
                {{ singer.singerName }}
              </span>
            </div>

            <div class="song-source">
              <a :href="getYoutubeUrlWithTimestamp(song.source.url, song.startAt)" target="_blank" rel="noopener noreferrer" @click.stop>
                {{ song.source.title }}
              </a>
            </div>
            <div class="song-duration">
              再生時間: {{ formatDuration(song.startAt) }} - {{ formatDuration(song.endAt) }}
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
</template>

<script setup lang="ts">
import type { Song } from '../types/song'
import YouTubePlayer from './YouTubePlayer.vue'

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

// プレイヤーを閉じる関数
const closePlayer = () => {
  selectedSong.value = null
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
watch(songs, (newSongs) => {
  if (newSongs) {
    console.log('=== 楽曲データ取得 ===')
    console.log(`楽曲数: ${newSongs.length}`)
    newSongs.forEach((song, index) => {
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
  max-width: 100%;
  margin: 0;
  padding: 0;
}

/* 16:9の動画ゾーン */
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
}

.player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
}

.player-info h3 {
  margin: 0 0 5px 0;
  font-size: 1.3rem;
  color: white;
}

.player-info p {
  margin: 0;
  color: #ccc;
  font-size: 0.95rem;
}

.close-button {
  background: #f44336;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 1.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
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
  font-size: 5rem;
  display: block;
  margin-bottom: 20px;
}

.placeholder-content p {
  font-size: 1.5rem;
  margin: 0;
  font-weight: 300;
}

/* 楽曲一覧セクション */
.list-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

h1 {
  font-size: 2rem;
  margin-bottom: 30px;
  color: #333;
}

/* 検索セクション */
.search-section {
  margin-bottom: 30px;
}

.search-input-container {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  font-size: 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #1976d2;
}

.search-button {
  padding: 12px 24px;
  font-size: 1rem;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.search-button:hover {
  background-color: #1565c0;
}

.clear-button {
  padding: 12px 20px;
  font-size: 1rem;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.clear-button:hover {
  background-color: #d32f2f;
}

.search-help {
  font-size: 0.85rem;
  color: #666;
  line-height: 1.5;
}

.search-help p {
  margin: 5px 0;
}

.search-help code {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  color: #d32f2f;
}

.loading, .error, .no-songs {
  text-align: center;
  padding: 40px;
  font-size: 1.2rem;
}

.error {
  color: #d32f2f;
}

.error button {
  margin-top: 20px;
  padding: 10px 20px;
  font-size: 1rem;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.error button:hover {
  background-color: #1565c0;
}

.song-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .song-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .song-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .song-list {
    grid-template-columns: 1fr;
  }
}

.song-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.song-card.clickable {
  cursor: pointer;
}

.song-card.clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.song-card.selected {
  border-color: #1976d2;
  border-width: 2px;
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
}

.song-thumbnail {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background-color: #f5f5f5;
  position: relative;
}

.song-thumbnail img {
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

.song-card.clickable:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 3rem;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.song-info {
  padding: 15px;
}

.song-title {
  font-size: 1.2rem;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: #333;
}

.song-artist {
  font-size: 0.95rem;
  color: #666;
  margin: 0 0 12px 0;
}

.song-singers {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.singer-tag {
  display: inline-block;
  padding: 4px 12px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-size: 0.85rem;
}


.song-source {
  margin-bottom: 8px;
  font-size: 0.9rem;
}

.song-source a {
  color: #1976d2;
  text-decoration: none;
}

.song-source a:hover {
  text-decoration: underline;
}

.song-duration {
  font-size: 0.85rem;
  color: #666;
}
</style>

