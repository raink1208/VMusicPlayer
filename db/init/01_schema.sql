-- ============================================
-- Music Database Schema (Refactored)
-- ============================================

-- 拡張機能の有効化
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================
-- マスターテーブル
-- ============================================

-- 音楽ソースタイプ
CREATE TABLE music_source_types (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 歌手
CREATE TABLE singers (
    id SERIAL PRIMARY KEY,
    public_id TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    youtube_url TEXT,
    twitter_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- プレイリスト
CREATE TABLE playlists (
    id SERIAL PRIMARY KEY,
    public_id TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- メインテーブル
-- ============================================

-- 音楽ソース
CREATE TABLE music_sources (
    id SERIAL PRIMARY KEY,
    public_id TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    upload_date DATE NOT NULL,
    type_id INTEGER NOT NULL REFERENCES music_source_types(id) ON DELETE RESTRICT,
    thumbnail_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 楽曲
CREATE TABLE songs (
    id SERIAL PRIMARY KEY,
    public_id TEXT UNIQUE NOT NULL,
    source_id INTEGER NOT NULL REFERENCES music_sources(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    artist TEXT,
    start_at INTERVAL NOT NULL,
    end_at INTERVAL NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_time_range CHECK (end_at > start_at)
);

-- ============================================
-- 中間テーブル
-- ============================================

-- 楽曲と歌手の関連
CREATE TABLE song_singers (
    song_id INTEGER NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    singer_id INTEGER NOT NULL REFERENCES singers(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (song_id, singer_id)
);

-- プレイリストと楽曲の関連
CREATE TABLE playlist_songs (
    playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    song_id INTEGER NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (playlist_id, song_id),
    CONSTRAINT check_position_positive CHECK (position > 0),
    UNIQUE (playlist_id, position)
);

-- ============================================
-- 初期データ
-- ============================================

INSERT INTO music_source_types (name) VALUES
    ('live'),
    ('video'),
    ('local');

-- ============================================
-- インデックス: 外部キー
-- ============================================

CREATE INDEX idx_music_sources_type_id ON music_sources(type_id);
CREATE INDEX idx_songs_source_id ON songs(source_id);
CREATE INDEX idx_song_singers_singer_id ON song_singers(singer_id);
CREATE INDEX idx_playlist_songs_song_id ON playlist_songs(song_id);

-- ============================================
-- インデックス: 検索最適化
-- ============================================

-- public_id検索用
CREATE INDEX idx_singers_public_id ON singers(public_id);
CREATE INDEX idx_playlists_public_id ON playlists(public_id);
CREATE INDEX idx_music_sources_public_id ON music_sources(public_id);
CREATE INDEX idx_songs_public_id ON songs(public_id);

-- 歌手名の部分一致検索用
CREATE INDEX idx_singers_name ON singers(name);
CREATE INDEX idx_singers_name_trgm ON singers USING gin(name gin_trgm_ops);

-- プレイリスト位置順ソート用
CREATE INDEX idx_playlist_songs_order ON playlist_songs(playlist_id, position);

-- ============================================
-- インデックス: 複合・カバリング
-- ============================================

-- 楽曲-歌手の結合クエリ最適化
CREATE INDEX idx_song_singers_covering ON song_singers(song_id, singer_id);

-- ソースタイプ別楽曲取得最適化
CREATE INDEX idx_music_sources_type_public ON music_sources(type_id, public_id);

-- ============================================
-- テーブルパフォーマンス設定
-- ============================================

ALTER TABLE songs SET (autovacuum_analyze_scale_factor = 0.05);
ALTER TABLE song_singers SET (autovacuum_analyze_scale_factor = 0.05);
ALTER TABLE playlist_songs SET (autovacuum_analyze_scale_factor = 0.05);

-- ============================================
-- コメント(ドキュメント)
-- ============================================

COMMENT ON TABLE music_source_types IS '音楽ソースの種類(ライブ、動画、ローカル等)';
COMMENT ON TABLE music_sources IS '音楽ソース(YouTube動画、ライブ配信等)';
COMMENT ON TABLE songs IS '楽曲(ソース内の特定時間範囲)';
COMMENT ON TABLE singers IS '歌手・アーティスト';
COMMENT ON TABLE playlists IS 'プレイリスト';
COMMENT ON TABLE song_singers IS '楽曲と歌手の多対多関連';
COMMENT ON TABLE playlist_songs IS 'プレイリストと楽曲の多対多関連';

COMMENT ON COLUMN songs.start_at IS '楽曲開始時間(INTERVALで秒数を表現)';
COMMENT ON COLUMN songs.end_at IS '楽曲終了時間(INTERVALで秒数を表現)';
COMMENT ON COLUMN playlist_songs.position IS 'プレイリスト内の表示順序(1から開始)';