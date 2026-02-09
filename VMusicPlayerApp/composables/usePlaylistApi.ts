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

  const createPlaylist = async (name: string, songIds: string[] = []): Promise<Playlist> => {
    return await $fetch<Playlist>(`${baseURL}/api/playlists`, {
      method: 'POST',
      body: { name, songIds }
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

  const updatePlaylistSongs = async (playlistId: string, songIds: string[]): Promise<boolean> => {
    try {
      await $fetch(`${baseURL}/api/playlists/${playlistId}/songs`, {
        method: 'PUT',
        body: { songIds }
      })
      return true
    } catch (error) {
      console.error('Failed to update playlist songs:', error)
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

  return {
    getAllPlaylists,
    getPlaylist,
    createPlaylist,
    updatePlaylistName,
    updatePlaylistSongs,
    deletePlaylist
  }
}

