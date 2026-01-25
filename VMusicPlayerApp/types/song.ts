export interface Singer {
  singerId: string
  singerName: string
  youtubeURL?: string
  twitterURL?: string
}

export interface MusicSource {
  sourceId: string
  title: string
  url: string
  uploadDate: string
  thumbnailUrl: string
  sourceType: string
}

export interface Song {
  songId: string
  title: string
  artist?: string
  startAt: string
  endAt: string
  source: MusicSource
  singers: Singer[]
}

