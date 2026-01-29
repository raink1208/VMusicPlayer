<template>
  <div class="playlist-detail-container">
    <div class="detail-header">
      <button @click="$emit('back')" class="back-button">
        ← 戻る
      </button>
      <h2 v-if="playlist">{{ playlist.name }}</h2>
      <div class="header-actions">
        <button @click="playAll" class="action-button primary" :disabled="!playlist || playlist.songs.length === 0">
          ▶️ すべて再生
        </button>
      </div>
    </div>

    <div v-if="pending" class="loading">
      読み込み中...
    </div>

    <div v-else-if="error" class="error">
      <p>プレイリストの読み込みに失敗しました</p>
      <button @click="() => refresh()">再読み込み</button>
    </div>

    <div v-else-if="playlist && playlist.songs.length > 0" class="song-list">
      <div
        v-for="item in playlist.songs"
        :key="item.song.songId"
        class="song-item"
        @click="playSong(item.song)"
      >
        <div class="item-position">{{ item.position }}</div>
        <div class="item-thumbnail">
          <img :src="item.song.source.thumbnailUrl" :alt="item.song.title" />
          <div class="play-overlay">
            <span class="play-icon">▶</span>
          </div>
        </div>
        <div class="item-content">
          <div class="item-title">{{ item.song.title }}</div>
          <div class="item-artist" v-if="item.song.artist">{{ item.song.artist }}</div>
          <div class="item-singers">
            <span v-for="singer in item.song.singers" :key="singer.singerId" class="singer-tag">
              {{ singer.singerName }}
            </span>
          </div>
        </div>
        <div class="item-actions" @click.stop>
          <button @click="addToQueue(item.song)" class="action-button" title="キューに追加">
            ➕
          </button>
          <button @click="confirmRemove(item.song)" class="action-button delete" title="削除">
            🗑️
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-playlist">
      <p>このプレイリストには楽曲がありません</p>
    </div>

    <!-- Remove Confirmation Dialog -->
    <div v-if="showRemoveDialog" class="modal-overlay" @click="showRemoveDialog = false">
      <div class="modal-content" @click.stop>
        <h3>楽曲を削除</h3>
        <p>プレイリストから「{{ removeTarget?.title }}」を削除してもよろしいですか？</p>
        <div class="modal-actions">
          <button @click="showRemoveDialog = false" class="cancel-button">キャンセル</button>
          <button @click="removeSong" class="confirm-button delete">削除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Song } from '../types/song'
import type { PlaylistDetail } from '../types/playlist'
import { usePlaylistApi } from '../composables/usePlaylistApi'

const props = defineProps<{
  playlistId: string
}>()

const emit = defineEmits<{
  'back': []
  'play-song': [song: Song, songs: Song[]]
  'add-to-queue': [song: Song]
}>()

const playlistApi = usePlaylistApi()

// Fetch playlist detail
const { data: playlist, pending, error, refresh } = await useFetch<PlaylistDetail>(
  () => `${useRuntimeConfig().public.apiBase || 'http://localhost:8080'}/api/playlists/${props.playlistId}`
)

// Play single song
const playSong = (song: Song) => {
  if (playlist.value) {
    const songs = playlist.value.songs.map(item => item.song)
    emit('play-song', song, songs)
  }
}

// Play all songs
const playAll = () => {
  if (playlist.value && playlist.value.songs.length > 0) {
    const songs = playlist.value.songs.map(item => item.song)
    emit('play-song', songs[0], songs)
  }
}

// Add to queue
const addToQueue = (song: Song) => {
  emit('add-to-queue', song)
}

// Remove song confirmation
const showRemoveDialog = ref(false)
const removeTarget = ref<Song | null>(null)

const confirmRemove = (song: Song) => {
  removeTarget.value = song
  showRemoveDialog.value = true
}

const removeSong = async () => {
  if (!removeTarget.value) return

  try {
    await playlistApi.removeSongFromPlaylist(props.playlistId, removeTarget.value.songId)
    showRemoveDialog.value = false
    removeTarget.value = null
    await refresh()
  } catch (error) {
    console.error('Failed to remove song:', error)
    alert('楽曲の削除に失敗しました')
  }
}
</script>

<style scoped>
.playlist-detail-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9f9f9;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.back-button {
  padding: 8px 16px;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.back-button:hover {
  background: #e0e0e0;
}

.detail-header h2 {
  flex: 1;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: #f0f0f0;
  color: #333;
}

.action-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-button.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-button.primary:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.action-button.delete:hover {
  background: #fee;
  color: #d33;
}

.action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.song-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.song-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.song-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.item-position {
  width: 32px;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #999;
}

.item-thumbnail {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
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

.song-item:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 32px;
  color: white;
}

.item-content {
  flex: 1;
  min-width: 0;
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

.item-actions .action-button {
  padding: 8px 12px;
  font-size: 14px;
}

.loading,
.error,
.empty-playlist {
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
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  min-width: 400px;
  max-width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-content h3 {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.modal-actions button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-button {
  background: #f0f0f0;
  color: #333;
}

.cancel-button:hover {
  background: #e0e0e0;
}

.confirm-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.confirm-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.confirm-button.delete {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.confirm-button.delete:hover {
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
}
</style>

