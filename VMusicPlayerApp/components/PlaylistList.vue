<template>
  <div class="playlist-list-container">
    <div class="playlist-header">
      <h2>プレイリスト</h2>
      <button @click="showCreateDialog = true" class="create-button">
        ➕ 新規作成
      </button>
    </div>

    <div v-if="pending" class="loading">
      読み込み中...
    </div>

    <div v-else-if="error" class="error">
      <p>エラーが発生しました: {{ error.message }}</p>
      <button @click="() => refresh()">再読み込み</button>
    </div>

    <div v-else-if="playlists && playlists.length > 0" class="playlist-grid">
      <div
        v-for="playlist in playlists"
        :key="playlist.playlistId"
        class="playlist-card"
        @click="$emit('select-playlist', playlist.playlistId)"
      >
        <div class="playlist-thumbnail">
          <img
            v-if="playlist.firstSongThumbnail"
            :src="playlist.firstSongThumbnail"
            :alt="playlist.name"
          />
          <div v-else class="no-thumbnail">
            <span class="icon">🎵</span>
          </div>
          <div class="play-overlay">
            <span class="play-icon">▶</span>
          </div>
        </div>
        <div class="playlist-info">
          <div class="playlist-name">{{ playlist.name }}</div>
          <div class="playlist-meta">{{ playlist.songCount }}曲</div>
        </div>
        <div class="playlist-actions" @click.stop>
          <button @click="editPlaylist(playlist)" class="action-button" title="名前を変更">
            ✏️
          </button>
          <button @click="confirmDelete(playlist)" class="action-button delete" title="削除">
            🗑️
          </button>
        </div>
      </div>
    </div>

    <div v-else class="no-playlists">
      <p>プレイリストがありません</p>
      <button @click="showCreateDialog = true" class="create-button">
        最初のプレイリストを作成
      </button>
    </div>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="modal-overlay" @click="showCreateDialog = false">
      <div class="modal-content" @click.stop>
        <h3>新しいプレイリスト</h3>
        <input
          v-model="newPlaylistName"
          type="text"
          placeholder="プレイリスト名"
          class="modal-input"
          @keyup.enter="createPlaylist"
        />
        <div class="modal-actions">
          <button @click="showCreateDialog = false" class="cancel-button">キャンセル</button>
          <button @click="createPlaylist" class="confirm-button" :disabled="!newPlaylistName.trim()">
            作成
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Dialog -->
    <div v-if="showEditDialog" class="modal-overlay" @click="showEditDialog = false">
      <div class="modal-content" @click.stop>
        <h3>プレイリスト名を変更</h3>
        <input
          v-model="editPlaylistName"
          type="text"
          placeholder="プレイリスト名"
          class="modal-input"
          @keyup.enter="updatePlaylist"
        />
        <div class="modal-actions">
          <button @click="showEditDialog = false" class="cancel-button">キャンセル</button>
          <button @click="updatePlaylist" class="confirm-button" :disabled="!editPlaylistName.trim()">
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <div v-if="showDeleteDialog" class="modal-overlay" @click="showDeleteDialog = false">
      <div class="modal-content" @click.stop>
        <h3>プレイリストを削除</h3>
        <p>「{{ deleteTarget?.name }}」を削除してもよろしいですか？</p>
        <div class="modal-actions">
          <button @click="showDeleteDialog = false" class="cancel-button">キャンセル</button>
          <button @click="deletePlaylist" class="confirm-button delete">削除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Playlist } from '../types/playlist'
import { usePlaylistApi } from '../composables/usePlaylistApi'

const emit = defineEmits<{
  'select-playlist': [playlistId: string]
}>()

const playlistApi = usePlaylistApi()

// Fetch playlists
const { data: playlists, pending, error, refresh } = await useFetch<Playlist[]>(
  () => `${useRuntimeConfig().public.apiBase || 'http://localhost:8080'}/api/playlists`
)

// Create dialog
const showCreateDialog = ref(false)
const newPlaylistName = ref('')

const createPlaylist = async () => {
  if (!newPlaylistName.value.trim()) return

  try {
    await playlistApi.createPlaylist(newPlaylistName.value)
    newPlaylistName.value = ''
    showCreateDialog.value = false
    await refresh()
  } catch (error) {
    console.error('Failed to create playlist:', error)
    alert('プレイリストの作成に失敗しました')
  }
}

// Edit dialog
const showEditDialog = ref(false)
const editPlaylistName = ref('')
const editTarget = ref<Playlist | null>(null)

const editPlaylist = (playlist: Playlist) => {
  editTarget.value = playlist
  editPlaylistName.value = playlist.name
  showEditDialog.value = true
}

const updatePlaylist = async () => {
  if (!editTarget.value || !editPlaylistName.value.trim()) return

  try {
    await playlistApi.updatePlaylistName(editTarget.value.playlistId, editPlaylistName.value)
    showEditDialog.value = false
    editTarget.value = null
    editPlaylistName.value = ''
    await refresh()
  } catch (error) {
    console.error('Failed to update playlist:', error)
    alert('プレイリスト名の変更に失敗しました')
  }
}

// Delete dialog
const showDeleteDialog = ref(false)
const deleteTarget = ref<Playlist | null>(null)

const confirmDelete = (playlist: Playlist) => {
  deleteTarget.value = playlist
  showDeleteDialog.value = true
}

const deletePlaylist = async () => {
  if (!deleteTarget.value) return

  try {
    await playlistApi.deletePlaylist(deleteTarget.value.playlistId)
    showDeleteDialog.value = false
    deleteTarget.value = null
    await refresh()
  } catch (error) {
    console.error('Failed to delete playlist:', error)
    alert('プレイリストの削除に失敗しました')
  }
}
</script>

<style scoped>
.playlist-list-container {
  padding: 20px;
}

.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.playlist-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.create-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.2s;
}

.create-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.playlist-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.playlist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.playlist-thumbnail {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.playlist-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-thumbnail {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-thumbnail .icon {
  font-size: 48px;
  opacity: 0.5;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.playlist-card:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 36px;
  color: white;
}

.playlist-info {
  padding: 12px;
}

.playlist-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-meta {
  font-size: 14px;
  color: #666;
}

.playlist-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
}

.action-button {
  flex: 1;
  padding: 6px;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.2s;
}

.action-button:hover {
  background: #e0e0e0;
}

.action-button.delete:hover {
  background: #fee;
  color: #d33;
}

.loading,
.error,
.no-playlists {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error button,
.no-playlists button {
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

.modal-input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.modal-input:focus {
  outline: none;
  border-color: #667eea;
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

.confirm-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.confirm-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.confirm-button.delete {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.confirm-button.delete:hover {
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
}
</style>

