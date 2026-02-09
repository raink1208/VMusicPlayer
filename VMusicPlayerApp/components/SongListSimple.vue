<template>
  <div class="song-list-container">
    <!-- 検索セクション -->
    <div class="search-section">
      <div class="search-input-container">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="楽曲を検索..."
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
          'selected': selectedSongId === song.songId,
          'clickable': song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'
        }"
      >
        <div class="item-thumbnail" @click="playSong(song)">
          <img :src="song.source.thumbnailUrl" :alt="song.title" />
          <div
            v-if="song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO'"
            class="play-overlay"
          >
            <span class="play-icon">▶</span>
          </div>
        </div>
        <div class="item-content" @click="playSong(song)">
          <div class="item-title">{{ song.title }}</div>
          <div class="item-artist" v-if="song.artist">{{ song.artist }}</div>
          <div class="item-singers">
            <span v-for="singer in song.singers" :key="singer.singerId" class="singer-tag">
              {{ singer.singerName }}
            </span>
          </div>
        </div>
        <div class="item-actions">
          <button @click="addToQueue(song)" class="action-button" title="キューに追加">
            ➕
          </button>
        </div>
      </div>
    </div>

    <div v-else class="no-songs">
      <p v-if="isSearchActive">検索結果が見つかりませんでした</p>
      <p v-else>楽曲が登録されていません</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Song } from '../types/song'

defineProps<{
  selectedSongId?: string
}>()

const emit = defineEmits<{
  'play-song': [song: Song, songs: Song[]]
  'add-to-queue': [song: Song]
}>()

const config = useRuntimeConfig()

// 検索関連の状態
const searchQuery = ref<string>('')
const isSearchActive = ref<boolean>(false)
const searchResults = ref<Song[] | null>(null)

// APIから曲一覧を取得
const { data: songs, pending, error, refresh } = await useFetch<Song[]>(
  `${config.public.apiBase}/api/songs`
)

// 表示する曲リストを計算
const displayedSongs = computed(() => {
  return isSearchActive.value && searchResults.value !== null
    ? searchResults.value
    : songs.value || []
})

// 楽曲を再生
const playSong = (song: Song) => {
  if (song.source.sourceType === 'LIVE' || song.source.sourceType === 'VIDEO') {
    emit('play-song', song, displayedSongs.value)
  }
}

// キューに追加
const addToQueue = (song: Song) => {
  emit('add-to-queue', song)
}

// 検索を実行
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    clearSearch()
    return
  }

  try {
    searchResults.value = await $fetch<Song[]>(
      `${config.public.apiBase}/api/search?q=${encodeURIComponent(searchQuery.value.trim())}`
    )
    isSearchActive.value = true
  } catch (err) {
    console.error('検索エラー:', err)
    searchResults.value = []
  }
}

// 検索をクリア
const clearSearch = () => {
  searchQuery.value = ''
  searchResults.value = null
  isSearchActive.value = false
}
</script>

<style scoped>
.song-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9f9f9;
}

.search-section {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.search-input-container {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.search-button,
.clear-button {
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.search-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.search-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.clear-button {
  background: #fee;
  color: #d33;
}

.clear-button:hover {
  background: #fdd;
}

.song-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.song-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 8px;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.song-list-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.song-list-item.selected {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border: 2px solid #667eea;
}

.item-thumbnail {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
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
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.song-list-item:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 32px;
  color: white;
}

.item-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-artist {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-singers {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.singer-tag {
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.item-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 8px 12px;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-button:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: scale(1.1);
}

.loading,
.error,
.no-songs {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
  color: #666;
}

.error button {
  margin-top: 16px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
</style>

