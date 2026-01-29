# プレイリスト機能実装完了

## 実装内容

### フロントエンド

1. **再生キュー機能** (`PlayQueue.vue`)
   - 楽曲の追加・削除・並び替え
   - ドラッグ&ドロップでの順序変更
   - キューからプレイリストへの保存
   - 連続再生のON/OFF切り替え

2. **プレイリスト管理** (`PlaylistList.vue`, `PlaylistDetail.vue`)
   - プレイリストの作成・編集・削除
   - プレイリストへの楽曲追加・削除
   - プレイリストからの再生

3. **統合UI** (`MusicPlayer.vue`)
   - タブベースのUI（楽曲リスト / 再生キュー / プレイリスト）
   - 連続再生機能
   - 前へ/次への楽曲移動

4. **Composables**
   - `usePlayQueue.ts`: 再生キュー管理
   - `usePlaylistApi.ts`: プレイリストAPI呼び出し

### バックエンド

1. **ドメインモデル**
   - `Playlist.kt`: プレイリスト基本情報
   - `PlaylistDetail.kt`: プレイリスト詳細（楽曲リスト含む）

2. **Repository** (`PlaylistRepository.kt`)
   - プレイリストの全取得
   - プレイリストの詳細取得
   - プレイリストの作成・更新・削除
   - プレイリストへの楽曲追加・削除・並び替え
   - キューからプレイリスト作成

3. **Service** (`PlaylistService.kt`)
   - ビジネスロジックの実装
   - ログ出力

4. **Controller** (`PlaylistController.kt`)
   - REST APIエンドポイント
   - `/api/playlists` - プレイリスト一覧取得・作成
   - `/api/playlists/{id}` - プレイリスト詳細取得・更新・削除
   - `/api/playlists/{id}/songs` - 楽曲追加
   - `/api/playlists/{id}/songs/{songId}` - 楽曲削除
   - `/api/playlists/{id}/songs/reorder` - 楽曲並び替え
   - `/api/playlists/from-queue` - キューからプレイリスト作成

## 使い方

### 開発サーバーの起動

**バックエンド:**
```bash
cd VMusicPlayerServer
./gradlew bootRun
```

**フロントエンド:**
```bash
cd VMusicPlayerApp
yarn install
yarn dev
```

### 機能の使い方

1. **連続再生**
   - プレイヤー下部の「🔁 連続再生」ボタンをクリックしてON/OFF
   - ONにすると楽曲終了時に自動的に次の楽曲を再生

2. **再生キュー**
   - 楽曲リストの「➕」ボタンでキューに追加
   - 「再生キュー」タブでキューを確認
   - ドラッグ&ドロップで順序変更
   - 「💾 保存」でプレイリストとして保存

3. **プレイリスト**
   - 「プレイリスト」タブで「➕ 新規作成」
   - プレイリストをクリックして詳細表示
   - 楽曲リストから「➕」でプレイリストに追加
   - 「▶️ すべて再生」で全楽曲を連続再生

## 依存関係

- **フロントエンド:**
  - vuedraggable@next: ドラッグ&ドロップ機能
  - sortablejs: ドラッグ&ドロップライブラリ

## 仕様書準拠

以下の仕様書に基づいて実装されています:
- `docs/continuous-playback-and-playlist-spec.md`

実装されたフェーズ:
- ✅ Phase 3: バックエンドAPI実装
- ✅ Phase 4: フロントエンドUI実装

## データベーススキーマ

プレイリスト関連のテーブル:
- `playlists`: プレイリストマスター
- `playlist_songs`: プレイリストと楽曲の中間テーブル

詳細は `db/init/01_schema.sql` を参照してください。

