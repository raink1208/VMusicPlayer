<template>
  <div class="app-container">
    <div class="main-layout">
      <!-- 左側: 動画プレイヤーセクション -->
      <div class="video-section">
        <div class="video-player-container">
          <div v-if="currentSong" class="video-player">
            <YouTubePlayer
              :key="playerKey"
              :video-id="extractYouTubeVideoId(currentSong.source.url)"
              :start-time="durationToSeconds(currentSong.startAt)"
              :end-time="durationToSeconds(currentSong.endAt)"
              :player-id="currentSong.songId"
              @video-ended="handleVideoEnded"
            />
          </div>
          <div v-else class="video-placeholder">
            <div class="placeholder-content">
              <span class="placeholder-icon">🎵</span>
              <p>楽曲を選択してください</p>
            </div>
          </div>
        </div>

        <!-- 再生コントロール -->
        <div class="player-controls">
          <button @click="playPrevious" :disabled="!hasPrevious" class="control-button">
            ⏮️ 前へ
          </button>
          <button
            @click="toggleContinuousPlay"
            class="continuous-play-toggle"
            :class="{ active: continuousPlayEnabled }"
          >
            🔁 連続再生 {{ continuousPlayEnabled ? 'ON' : 'OFF' }}
          </button>
          <button @click="playNext" :disabled="!hasNext" class="control-button">
            次へ ⏭️
          </button>
        </div>
      </div>

      <!-- 右側: コンテンツセクション -->
      <div class="content-section">
        <!-- タブナビゲーション -->
        <div class="tab-navigation">
          <button
            @click="activeTab = 'songs'"
            class="tab-button"
            :class="{ active: activeTab === 'songs' }"
          >
            🎵 楽曲リスト
          </button>
          <button
            @click="activeTab = 'queue'"
            class="tab-button"
            :class="{ active: activeTab === 'queue' }"
          >
            📋 再生キュー ({{ queueLength }})
          </button>
          <button
            @click="activeTab = 'playlists'"
            class="tab-button"
            :class="{ active: activeTab === 'playlists' }"
          >
            💾 プレイリスト
          </button>
        </div>

        <!-- タブコンテンツ -->
        <div class="tab-content">
          <!-- 楽曲リストタブ -->
          <div v-show="activeTab === 'songs'" class="tab-pane">
            <SongListSimple
              :selected-song-id="currentSong?.songId"
              @play-song="playSong"
              @add-to-queue="addToQueue"
            />
          </div>

          <!-- 再生キュータブ -->
          <div v-show="activeTab === 'queue'" class="tab-pane">
            <PlayQueue @play-song="playSongFromQueue" />
          </div>

          <!-- プレイリストタブ -->
          <div v-show="activeTab === 'playlists'" class="tab-pane">
            <PlaylistList
              v-if="!selectedPlaylistId"
              @select-playlist="selectPlaylist"
            />
            <PlaylistDetail
              v-else
              :playlist-id="selectedPlaylistId"
              @back="selectedPlaylistId = null"
              @play-song="playSong"
              @add-to-queue="addToQueue"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Song } from '../types/song'
import { usePlayQueue } from '../composables/usePlayQueue'
import YouTubePlayer from './YouTubePlayer.vue'
import SongListSimple from './SongListSimple.vue'
import PlayQueue from './PlayQueue.vue'
import PlaylistList from './PlaylistList.vue'
import PlaylistDetail from './PlaylistDetail.vue'

const {
  queueLength,
  continuousPlayEnabled,
  hasNext,
  hasPrevious,
  addToQueue: addSongToQueue,
  setQueue,
  setCurrentIndex,
  playNext: playNextInQueue,
  playPrevious: playPreviousInQueue,
  toggleContinuousPlay
} = usePlayQueue()

const activeTab = ref<'songs' | 'queue' | 'playlists'>('songs')
const currentSong = ref<Song | null>(null)
const playerKey = ref(0)
const selectedPlaylistId = ref<string | null>(null)

// YouTube URLから動画IDを抽出
const extractYouTubeVideoId = (url: string): string => {
  if (!url) return ''

  const watchMatch = url.match(/[?&]v=([^&]+)/)
  if (watchMatch && watchMatch[1]) return watchMatch[1]

  const shortMatch = url.match(/youtu\.be\/([^?]+)/)
  if (shortMatch && shortMatch[1]) return shortMatch[1]

  const liveMatch = url.match(/youtube\.com\/live\/([^?]+)/)
  if (liveMatch && liveMatch[1]) return liveMatch[1]

  return ''
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

// 楽曲を再生
const playSong = (song: Song, songs?: Song[]) => {
  currentSong.value = song
  playerKey.value++

  // 楽曲リストが提供されている場合、キューとして設定
  if (songs && songs.length > 0) {
    const index = songs.findIndex(s => s.songId === song.songId)
    setQueue(songs, index >= 0 ? index : 0)
  }
}

// キューから楽曲を再生
const playSongFromQueue = (song: Song, index: number) => {
  currentSong.value = song
  setCurrentIndex(index)
  playerKey.value++
}

// キューに追加
const addToQueue = (song: Song) => {
  addSongToQueue(song)
  // 通知を表示（オプション）
}

// 次の曲を再生
const playNext = () => {
  const nextSong = playNextInQueue()
  if (nextSong) {
    currentSong.value = nextSong
    playerKey.value++
  }
}

// 前の曲を再生
const playPrevious = () => {
  const prevSong = playPreviousInQueue()
  if (prevSong) {
    currentSong.value = prevSong
    playerKey.value++
  }
}

// 動画終了時の処理
const handleVideoEnded = () => {
  if (continuousPlayEnabled.value && hasNext.value) {
    playNext()
  }
}

// プレイリストを選択
const selectPlaylist = (playlistId: string) => {
  selectedPlaylistId.value = playlistId
}
</script>

<style scoped>
.app-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
}

.main-layout {
  display: flex;
  width: 100%;
  height: 100%;
}

.video-section {
  width: 40%;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  padding: 20px;
}

.video-player-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
}

.video-player {
  width: 100%;
  height: 100%;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.placeholder-content {
  text-align: center;
}

.placeholder-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.3;
}

.player-controls {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  justify-content: center;
}

.control-button,
.continuous-play-toggle {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.control-button {
  background: #2a2a2a;
  color: white;
}

.control-button:hover:not(:disabled) {
  background: #3a3a3a;
  transform: translateY(-2px);
}

.control-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.continuous-play-toggle {
  background: #2a2a2a;
  color: #999;
}

.continuous-play-toggle.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.continuous-play-toggle:hover {
  transform: translateY(-2px);
}

.content-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  overflow: hidden;
}

.tab-navigation {
  display: flex;
  border-bottom: 2px solid #e0e0e0;
  background: white;
}

.tab-button {
  flex: 1;
  padding: 16px;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 3px solid transparent;
}

.tab-button:hover {
  background: #f9f9f9;
  color: #333;
}

.tab-button.active {
  color: #667eea;
  border-bottom-color: #667eea;
  background: #f9f9f9;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}

.tab-pane {
  height: 100%;
  overflow-y: auto;
}
</style>

