<template>
  <div class="youtube-player-container">
    <div :id="`youtube-player-${playerId}`" class="youtube-player"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  videoId: string
  startTime?: number
  endTime?: number
  playerId: string
}

const props = defineProps<Props>()

const player = ref<any>(null)
const playerReady = ref(false)

// YouTube IFrame APIをロード
const loadYouTubeAPI = () => {
  return new Promise<void>((resolve) => {
    if ((window as any).YT && (window as any).YT.Player) {
      resolve()
      return
    }

    const tag = document.createElement('script')
    tag.src = 'https://www.youtube.com/iframe_api'
    const firstScriptTag = document.getElementsByTagName('script')[0]
    if (firstScriptTag && firstScriptTag.parentNode) {
      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)
    } else {
      document.head.appendChild(tag)
    }

    ;(window as any).onYouTubeIframeAPIReady = () => {
      resolve()
    }
  })
}

// プレイヤーを初期化
const initPlayer = async () => {
  await loadYouTubeAPI()

  player.value = new (window as any).YT.Player(`youtube-player-${props.playerId}`, {
    height: '100%',
    width: '100%',
    videoId: props.videoId,
    playerVars: {
      start: props.startTime || 0,
      end: props.endTime || 0,
      autoplay: 1,
      modestbranding: 1,
      rel: 0,
    },
    events: {
      onReady: (event: any) => {
        playerReady.value = true
        console.log('YouTube player ready', event)
      },
      onStateChange: (event: any) => {
        // 終了時刻に達したら停止
        if (event.data === (window as any).YT.PlayerState.PLAYING && props.endTime) {
          checkTime()
        }
      },
    },
  })
}

// 再生時刻をチェック
const checkTime = () => {
  if (!player.value || !props.endTime) return

  const interval = setInterval(() => {
    if (player.value && player.value.getCurrentTime) {
      const currentTime = player.value.getCurrentTime()
      if (currentTime >= (props.endTime || 0)) {
        player.value.pauseVideo()
        clearInterval(interval)
      }
    }
  }, 100)
}

onMounted(() => {
  initPlayer()
})

onUnmounted(() => {
  if (player.value && player.value.destroy) {
    player.value.destroy()
  }
})

// videoIdが変更された場合は再初期化
watch(() => props.videoId, () => {
  if (player.value && player.value.loadVideoById) {
    player.value.loadVideoById({
      videoId: props.videoId,
      startSeconds: props.startTime || 0,
      endSeconds: props.endTime || 0,
    })
  }
})
</script>

<style scoped>
.youtube-player-container {
  width: 100%;
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.youtube-player {
  width: 100%;
  height: 100%;
  flex: 1;
}
</style>

