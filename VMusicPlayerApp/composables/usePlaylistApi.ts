import type { Playlist, PlaylistDetail } from '../types/playlist'

export const usePlaylistApi = () => {
  const config = useRuntimeConfig()
  const baseURL = config.public.apiBase || 'http://localhost:8080'

  const getAllPlaylists = async (): Promise<Playlist[]> => {
    return await $fetch<Playlist[]>(`${baseURL}/api/playlists`)
  }

  const getPlaylist = async (playlistId: string): Promise<PlaylistDetail | null> => {
    try {
      return await $fetch<PlaylistDetail>(`${baseURL}/api/playlists/${playlistId}`)
    } catch (error) {
      console.error('Failed to fetch playlist:', error)
      return null
    }
  }

  const createPlaylist = async (name: string): Promise<Playlist> => {
    return await $fetch<Playlist>(`${baseURL}/api/playlists`, {
      method: 'POST',
      body: { name }
    })
  }

  const updatePlaylistName = async (playlistId: string, name: string): Promise<boolean> => {
    try {
      await $fetch(`${baseURL}/api/playlists/${playlistId}`, {
        method: 'PUT',
        body: { name }
      })
      return true
    } catch (error) {
      console.error('Failed to update playlist:', error)
      return false
    }
  }

  const deletePlaylist = async (playlistId: string): Promise<boolean> => {
    try {
      await $fetch(`${baseURL}/api/playlists/${playlistId}`, {
        method: 'DELETE'
      })
      return true
    } catch (error) {
      console.error('Failed to delete playlist:', error)
      return false
    }
  }

  const addSongToPlaylist = async (playlistId: string, songId: string): Promise<number> => {
    const result = await $fetch<{ position: number }>(`${baseURL}/api/playlists/${playlistId}/songs`, {
      method: 'POST',
      body: { songId }
    })
    return result.position
  }

  const removeSongFromPlaylist = async (playlistId: string, songId: string): Promise<boolean> => {
    try {
      await $fetch(`${baseURL}/api/playlists/${playlistId}/songs/${songId}`, {
        method: 'DELETE'
      })
      return true
    } catch (error) {
      console.error('Failed to remove song from playlist:', error)
      return false
    }
  }

  const reorderPlaylistSongs = async (playlistId: string, songIds: string[]): Promise<boolean> => {
    try {
      await $fetch(`${baseURL}/api/playlists/${playlistId}/songs/reorder`, {
        method: 'PUT',
        body: { songIds }
      })
      return true
    } catch (error) {
      console.error('Failed to reorder playlist songs:', error)
      return false
    }
  }

  const createPlaylistFromQueue = async (name: string, songIds: string[]): Promise<Playlist> => {
    return await $fetch<Playlist>(`${baseURL}/api/playlists/from-queue`, {
      method: 'POST',
      body: { name, songIds }
    })
  }

  return {
    getAllPlaylists,
    getPlaylist,
    createPlaylist,
    updatePlaylistName,
    deletePlaylist,
    addSongToPlaylist,
    removeSongFromPlaylist,
    reorderPlaylistSongs,
    createPlaylistFromQueue
  }
}

