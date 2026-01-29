<template>
  <div class="play-queue-container">
    <div class="queue-header">
      <h3>再生キュー ({{ queueLength }}曲)</h3>
      <div class="queue-actions">
        <button
          @click="toggleContinuousPlay"
          class="continuous-play-button"
          :class="{ active: continuousPlayEnabled }"
        >
          🔁 連続再生 {{ continuousPlayEnabled ? 'ON' : 'OFF' }}
        </button>
        <button @click="showSaveDialog = true" class="save-button" :disabled="queueLength === 0">
          💾 保存
        </button>
        <button @click="clearQueue" class="clear-button" :disabled="queueLength === 0">
          🗑️ クリア
        </button>
      </div>
    </div>

    <div v-if="queueLength === 0" class="empty-queue">
      <p>再生キューが空です</p>
      <p class="hint">楽曲リストから「+」ボタンで追加できます</p>
    </div>

    <div v-else class="queue-list">
      <draggable
        v-model="queueSongs"
        item-key="songId"
        @end="onReorder"
        class="draggable-list"
      >
        <template #item="{ element, index }">
          <div
            class="queue-item"
            :class="{
              'current': index === currentIndex,
              'next': index === currentIndex + 1 && continuousPlayEnabled
            }"
            @click="playSong(index)"
          >
            <div class="drag-handle">⋮⋮</div>
            <div class="item-thumbnail">
              <img :src="element.source.thumbnailUrl" :alt="element.title" />
              <div v-if="index === currentIndex" class="playing-indicator">
                <span class="icon">▶</span>
              </div>
              <div v-else-if="index === currentIndex + 1 && continuousPlayEnabled" class="next-indicator">
                <span class="badge">次</span>
              </div>
            </div>
            <div class="item-content">
              <div class="item-title">{{ element.title }}</div>
              <div class="item-artist" v-if="element.artist">{{ element.artist }}</div>
              <div class="item-singers">
                <span v-for="singer in element.singers" :key="singer.singerId" class="singer-tag">
                  {{ singer.singerName }}
                </span>
              </div>
            </div>
            <button @click.stop="removeFromQueue(element.songId)" class="remove-button">
              ✕
            </button>
          </div>
        </template>
      </draggable>
    </div>

    <!-- Save to Playlist Dialog -->
    <div v-if="showSaveDialog" class="modal-overlay" @click="showSaveDialog = false">
      <div class="modal-content" @click.stop>
        <h3>キューをプレイリストとして保存</h3>
        <input
          v-model="newPlaylistName"
          type="text"
          placeholder="プレイリスト名"
          class="modal-input"
          @keyup.enter="saveAsPlaylist"
        />
        <div class="modal-actions">
          <button @click="showSaveDialog = false" class="cancel-button">キャンセル</button>
          <button @click="saveAsPlaylist" class="confirm-button" :disabled="!newPlaylistName.trim()">
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import draggable from 'vuedraggable'
import type { Song } from '../types/song'
import { usePlayQueue } from '../composables/usePlayQueue'
import { usePlaylistApi } from '../composables/usePlaylistApi'

const emit = defineEmits<{
  'play-song': [song: Song, index: number]
}>()

const {
  queue,
  currentIndex,
  continuousPlayEnabled,
  queueLength,
  removeFromQueue,
  clearQueue,
  reorderQueue,
  toggleContinuousPlay,
  setCurrentIndex
} = usePlayQueue()

const playlistApi = usePlaylistApi()

// For draggable
const queueSongs = computed({
  get: () => queue.value,
  set: (value) => reorderQueue(value)
})

const onReorder = () => {
  // Queue has been reordered via draggable
}

const playSong = (index: number) => {
  const song = queue.value[index]
  if (song) {
    setCurrentIndex(index)
    emit('play-song', song, index)
  }
}

// Save as playlist
const showSaveDialog = ref(false)
const newPlaylistName = ref('')

const saveAsPlaylist = async () => {
  if (!newPlaylistName.value.trim() || queueLength.value === 0) return

  try {
    const songIds = queue.value.map(song => song.songId)
    await playlistApi.createPlaylistFromQueue(newPlaylistName.value, songIds)
    newPlaylistName.value = ''
    showSaveDialog.value = false
    alert('プレイリストを作成しました')
  } catch (error) {
    console.error('Failed to save playlist:', error)
    alert('プレイリストの保存に失敗しました')
  }
}
</script>

<style scoped>
.play-queue-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9f9f9;
}

.queue-header {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.queue-header h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
}

.queue-actions {
  display: flex;
  gap: 8px;
}

.queue-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.continuous-play-button {
  background: #e0e0e0;
  color: #666;
}

.continuous-play-button.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.save-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.save-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.clear-button {
  background: #fee;
  color: #d33;
}

.clear-button:hover:not(:disabled) {
  background: #fdd;
}

.queue-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-queue {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
  color: #999;
}

.empty-queue .hint {
  font-size: 14px;
  margin-top: 8px;
}

.queue-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.draggable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.queue-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.queue-item.current {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border: 2px solid #667eea;
}

.queue-item.next {
  background: linear-gradient(135deg, #11998e15 0%, #38ef7d15 100%);
  border: 2px solid #11998e;
}

.drag-handle {
  color: #ccc;
  cursor: grab;
  font-size: 20px;
  user-select: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.item-thumbnail {
  position: relative;
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
}

.item-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playing-indicator,
.next-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.8);
}

.playing-indicator .icon {
  font-size: 24px;
  color: white;
}

.next-indicator {
  background: rgba(17, 153, 142, 0.8);
}

.next-indicator .badge {
  padding: 4px 8px;
  background: white;
  color: #11998e;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-artist {
  font-size: 12px;
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
  padding: 2px 6px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 11px;
  color: #666;
}

.remove-button {
  padding: 4px 8px;
  background: #f0f0f0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  transition: all 0.2s;
}

.remove-button:hover {
  background: #fee;
  color: #d33;
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
</style>

