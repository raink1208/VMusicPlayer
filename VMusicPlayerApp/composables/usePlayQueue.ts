import { ref, computed } from 'vue'
import type { Song } from '../types/song'

const queue = ref<Song[]>([])
const currentIndex = ref(-1)
const continuousPlayEnabled = ref(false)

export const usePlayQueue = () => {
  // ローカルストレージから連続再生設定を読み込む
  if (import.meta.client) {
    const saved = localStorage.getItem('continuousPlayEnabled')
    if (saved !== null) {
      continuousPlayEnabled.value = JSON.parse(saved)
    }
  }

  const addToQueue = (song: Song) => {
    if (!queue.value.find(s => s.songId === song.songId)) {
      queue.value.push(song)
    }
  }

  const removeFromQueue = (songId: string) => {
    const index = queue.value.findIndex(s => s.songId === songId)
    if (index !== -1) {
      queue.value.splice(index, 1)
      if (currentIndex.value >= index && currentIndex.value > 0) {
        currentIndex.value--
      }
    }
  }

  const clearQueue = () => {
    queue.value = []
    currentIndex.value = -1
  }

  const setQueue = (songs: Song[], startIndex: number = 0) => {
    queue.value = [...songs]
    currentIndex.value = startIndex
  }

  const setCurrentIndex = (index: number) => {
    if (index >= 0 && index < queue.value.length) {
      currentIndex.value = index
    }
  }

  const reorderQueue = (newOrder: Song[]) => {
    queue.value = newOrder
  }

  const getNextSong = (): Song | null => {
    if (!continuousPlayEnabled.value) return null
    if (currentIndex.value < 0 || currentIndex.value >= queue.value.length - 1) return null
    return queue.value[currentIndex.value + 1] || null
  }

  const getPreviousSong = (): Song | null => {
    if (currentIndex.value <= 0) return null
    return queue.value[currentIndex.value - 1] || null
  }

  const playNext = (): Song | null => {
    const nextSong = getNextSong()
    if (nextSong) {
      currentIndex.value++
    }
    return nextSong
  }

  const playPrevious = (): Song | null => {
    const prevSong = getPreviousSong()
    if (prevSong) {
      currentIndex.value--
    }
    return prevSong
  }

  const toggleContinuousPlay = () => {
    continuousPlayEnabled.value = !continuousPlayEnabled.value
    if (import.meta.client) {
      localStorage.setItem('continuousPlayEnabled', JSON.stringify(continuousPlayEnabled.value))
    }
  }

  const getCurrentSong = computed(() => {
    if (currentIndex.value >= 0 && currentIndex.value < queue.value.length) {
      return queue.value[currentIndex.value]
    }
    return null
  })

  const queueLength = computed(() => queue.value.length)

  const hasNext = computed(() => {
    return continuousPlayEnabled.value &&
           currentIndex.value >= 0 &&
           currentIndex.value < queue.value.length - 1
  })

  const hasPrevious = computed(() => {
    return currentIndex.value > 0
  })

  return {
    queue: computed(() => queue.value),
    currentIndex: computed(() => currentIndex.value),
    continuousPlayEnabled: computed(() => continuousPlayEnabled.value),
    getCurrentSong,
    queueLength,
    hasNext,
    hasPrevious,
    addToQueue,
    removeFromQueue,
    clearQueue,
    setQueue,
    setCurrentIndex,
    reorderQueue,
    getNextSong,
    getPreviousSong,
    playNext,
    playPrevious,
    toggleContinuousPlay
  }
}

