import type { Song } from './song'

export interface Playlist {
  playlistId: string
  name: string
  songCount: number
  firstSongThumbnail?: string
  createdAt: string
  updatedAt: string
}

export interface PlaylistSongItem {
  position: number
  song: Song
}

export interface PlaylistDetail {
  playlistId: string
  name: string
  songs: PlaylistSongItem[]
  createdAt: string
  updatedAt: string
}

export interface PlayQueue {
  songs: Song[]
  currentIndex: number
}

