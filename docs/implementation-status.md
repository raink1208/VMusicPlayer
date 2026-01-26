# VMusicPlayer 実装状態レポート

**作成日**: 2026-01-19  
**プロジェクト**: VMusicPlayer  
**アーキテクチャ**: マイクロサービス（Spring Boot + Nuxt.js + PostgreSQL）

---

## 📋 プロジェクト概要

VMusicPlayerは、音楽ソース（YouTube動画等）から楽曲を管理・再生するためのWebアプリケーションです。

### 技術スタック

| レイヤー | 技術 | バージョン |
|---------|------|-----------|
| フロントエンド | Nuxt.js | 4.0.1 |
| バックエンド | Spring Boot + Kotlin | 3.5.3 / 1.9.25 |
| データベース | PostgreSQL | 18 |
| コンテナ | Docker Compose | 3.8 |
| ビルドツール | Gradle | - |
| パッケージマネージャ | Yarn | 1.22.22 |

---

## 🗄️ データベース設計

### 完成度: **95%**

#### テーブル構成

```
music_source_types (ソースタイプマスタ)
  ├── music_sources (音楽ソース)
  │     └── songs (楽曲)
  │           └── song_singers (楽曲-歌手関連)
  │                 └── singers (歌手)
  │
  └── playlists (プレイリスト)
        └── playlist_songs (プレイリスト-楽曲関連)
```

#### 主要テーブル詳細

**music_sources** - 音楽ソース
- public_id (ULID), title, url, upload_date, type_id, thumbnail_url
- YouTube動画、ライブ配信などの情報を保存

**songs** - 楽曲
- public_id (ULID), source_id, title, artist, start_at, end_at
- 音楽ソース内の特定時間範囲を楽曲として定義

**singers** - 歌手
- public_id (ULID), name, youtube_url, twitter_url
- 複数の楽曲と多対多関連

**playlists** - プレイリスト
- public_id (ULID), name
- 楽曲の順序付きリスト

#### データベースの特徴

✅ **実装済み機能**
- ULID（Universally Unique Lexicographically Sortable Identifier）による外部ID管理
- pg_trgm拡張による高速全文検索（歌手名の部分一致検索）
- 適切なインデックス設計
  - 外部キーインデックス
  - 検索最適化インデックス（GIN, B-tree）
  - カバリングインデックス
- ON DELETE CASCADE/RESTRICT による整合性制約
- autovacuum最適化設定
- CHECK制約（時間範囲、位置の妥当性）
- 包括的なテーブル・カラムコメント

---

## 🔧 バックエンド実装状態

### 完成度: **70%**

### ディレクトリ構成

```
src/main/kotlin/com/github/rain1208/vmusicplayerserver/
├── VMusicPlayerServerApplication.kt (エントリポイント)
├── controller/ (❌ 未実装)
│   └── dto/
│       └── RegisterSongRequest.kt
├── domain/
│   ├── models/ (✅ 完成)
│   │   ├── Song.kt
│   │   ├── Singer.kt
│   │   ├── MusicSource.kt
│   │   └── MusicSourceType.kt
│   └── repository/ (インターフェース定義)
│       ├── ISongRepository.kt
│       ├── ISingerRepository.kt
│       ├── IMusicSourceRepository.kt
│       ├── ISearchRepository.kt
│       └── IPlaylistRepository.kt
├── repository/ (✅ 80%完成)
│   ├── SongRepository.kt (完成)
│   ├── SingerRepository.kt (完成)
│   ├── MusicSourceRepository.kt (完成)
│   ├── SearchRepository.kt (完成)
│   ├── PlaylistRepository.kt (❌ 空実装)
│   ├── mapper/
│   │   ├── SongRowMapper.kt
│   │   ├── SingerRowMapper.kt
│   │   └── MusicSourceRowMapper.kt
│   └── extension/
│       └── ResultSetExtension.kt
├── services/ (⚠️ 部分実装)
│   ├── SongService.kt (完成)
│   └── SearchService.kt (完成)
└── exception/ (❌ 未実装)
```

---

### 実装詳細

#### ✅ ドメインモデル層（100%完成）

**Song.kt**
```kotlin
data class Song(
    val songId: String,        // ULID
    val title: String,
    val artist: String?,
    val startAt: Duration,     // 楽曲開始時間
    val endAt: Duration,       // 楽曲終了時間
    val source: MusicSource,   // 関連する音楽ソース
    val singers: List<Singer>  // 歌手リスト
)
```

**Singer.kt**
```kotlin
data class Singer(
    val singerId: String,      // ULID
    val singerName: String,
    val youtubeURL: String?,
    val twitterURL: String?
)
```

**MusicSource.kt**
```kotlin
data class MusicSource(
    val sourceId: String,      // ULID
    val title: String,
    val url: String,
    val uploadDate: String,
    val thumbnailUrl: String,
    val sourceType: MusicSourceType
)
```

---

#### ✅ リポジトリ層（80%完成）

##### 完全実装済み

**1. SongRepository.kt**
- `getAllSongs()`: 全楽曲取得（JSON集約で歌手リストを効率的に取得）
- `getSong(songId)`: 単一楽曲取得
- `saveSong(song)`: UPSERT + 歌手関連の自動同期
- `deleteSong(songId)`: 楽曲削除

**特徴**:
- CTEを活用した複雑なSQL
- JSON_AGGによるN+1問題の回避
- UPSERT + 差分更新による冪等性保証

**2. SingerRepository.kt**
- `getAllSingers()`: 全歌手取得
- `saveSinger(singer)`: UPSERT対応
- `deleteSinger(singerId)`: 歌手削除

**3. MusicSourceRepository.kt**
- `getAllMusicSources()`: 全音楽ソース取得
- `saveMusicSource(musicSource)`: UPSERT対応
- `deleteMusicSource(musicSourceId)`: 音楽ソース削除

**4. SearchRepository.kt**
- `searchBySongTitle(songTitle)`: 楽曲タイトルで検索（部分一致）
- `searchBySongArtist(songArtist)`: アーティストで検索（部分一致）
- `searchBySingerName(singerName)`: 歌手名で検索（部分一致）
- `getSongsByMusicSourceId(musicSourceId)`: 音楽ソースIDから楽曲取得
- `getSongsBySingerId(singerId)`: 歌手IDから楽曲取得

##### ❌ 未実装

**PlaylistRepository.kt**
- クラス宣言のみ（空実装）
- プレイリストCRUD機能が必要

---

#### ⚠️ サービス層（40%完成）

##### 実装済み

**1. SongService.kt**
```kotlin
@Service
class SongService(private val songRepository: SongRepository) {
    fun getAllSongs(): List<Song>
    fun getSong(songId: String): Song?
    fun saveSong(song: Song)
    fun deleteSong(songId: String)
}
```

**2. SearchService.kt**
```kotlin
@Service
class SearchService(private val searchRepository: SearchRepository) {
    fun searchBySongTitle(songTitle: String): List<Song>
    fun searchBySongArtist(songArtist: String): List<Song>
    fun searchByMusicSourceId(musicSourceId: String): List<Song>
    fun searchBySingerId(singerId: String): List<Song>
    fun searchBySingerName(singerName: String): List<Song>
}
```
- SLF4Jによるロギング実装済み

##### ❌ 未実装

- PlaylistService
- MusicSourceService
- SingerService

---

#### ❌ コントローラ層（0%完成）

**致命的な問題: REST APIエンドポイントが存在しない**

現在、`@RestController`アノテーションを持つクラスが一切存在しません。
つまり、外部からHTTPアクセスができない状態です。

**必要な実装**:
- SongController (CRUD + 検索)
- SingerController (CRUD)
- MusicSourceController (CRUD)
- PlaylistController (CRUD)
- SearchController (統合検索API)

**DTOクラスは一部存在**:
- `RegisterSongRequest.kt` - 楽曲登録用DTO

---

#### ✅ テスト（部分実装）

**実装済みテストクラス**:
- `SongRepositoryTest.kt` - 基本的なCRUD操作テスト
- `SingerRepositoryTest.kt`
- `SearchRepositoryTest.kt`
- `MusicSourceRepositoryTest.kt`
- `VMusicPlayerServerApplicationTests.kt` - 起動テスト

**テストカバレッジ**: リポジトリ層のみ、サービス・コントローラ層は未テスト

---

### 設定ファイル

**application.properties**
```ini
spring.application.name=VMusicPlayerServer
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/VMusicPlayer
spring.datasource.username=VMusicPlayer
spring.datasource.password=VMusicPlayer
```

**build.gradle.kts**
- Spring Boot 3.5.3
- Kotlin 1.9.25
- ULID ライブラリ (de.huxhorn.sulky)
- PostgreSQL JDBC ドライバ
- Spring JDBC

---

## 🎨 フロントエンド実装状態

### 完成度: **5%**

### 現状

❌ **ほぼ未着手状態**

**存在するファイル**:
- `app/app.vue` - デフォルトのNuxtWelcomeコンポーネントのみ表示
- `nuxt.config.ts` - 最小限の設定のみ

**未実装要素**:
- ページコンポーネント（pages/）
- カスタムコンポーネント（components/）
- Composables（API通信ロジック）
- 状態管理（Pinia等）
- ルーティング
- UIライブラリの選定・導入

### 依存関係

```json
{
  "dependencies": {
    "nuxt": "^4.0.1",
    "vue": "^3.5.17",
    "vue-router": "^4.5.1"
  },
  "devDependencies": {
    "@nuxt/eslint": "^1.7.0",
    "eslint": "^9.0.0",
    "prettier": "^3.6.2",
    "typescript": "^5.8.3"
  }
}
```

**特徴**:
- ✅ ESLint + Prettier設定済み
- ✅ TypeScript設定済み
- ✅ Nuxt 4の最新機能対応

---

## 🐳 インフラ・Docker構成

### 完成度: **100%**

### Docker Compose 構成

```yaml
services:
  backend:
    - Spring Boot アプリケーション
    - ポート: 8080 (HTTP), 6565 (デバッグ?)
    - 環境変数でDB接続設定
    
  frontend:
    - Nuxt.js アプリケーション
    - ポート: 3000
    
  db:
    - PostgreSQL 18
    - ポート: 5432
    - 初期化スクリプト: db/init/01_schema.sql
    - 永続化ボリューム: vmusic-pgdata
```

**ネットワーク**: app-network（内部通信用）

**特徴**:
- ✅ サービス間依存関係の定義（depends_on）
- ✅ データベース初期化の自動化
- ✅ 開発環境用の適切なポート露出
- ✅ 環境変数による設定外部化

---

## 🛠️ ツール・ユーティリティ

### InsertTools - データ投入ツール

### 完成度: **100%**

**言語**: Python 3  
**ファイル**: `InsertTools/insert.py`

#### 機能
- JSONファイルから音楽データをPostgreSQLに一括投入
- ULID自動生成
- 時間表記の柔軟なパース（`h:mm:ss`, `m:ss`, `ss`）
- 冪等性保証（既存データの再利用）
- Natural Key（URL、名前、タイムスタンプ）による重複回避

#### データファイル
- `MementoV_music_sources.json`
- `Tengoku_Amane_sources.json`
- `Tsukasa_music_sources.json`

**使用ライブラリ**:
- psycopg (PostgreSQL接続)
- python-dateutil (日付パース)
- ulid-py (ULID生成)

---

## 📊 実装進捗サマリ

| コンポーネント | 完成度 | 状態 | 備考 |
|--------------|-------|------|------|
| **データベース** | 95% | ✅ ほぼ完成 | Playlistリポジトリ待ち |
| **バックエンド - ドメイン** | 100% | ✅ 完成 | モデル定義完了 |
| **バックエンド - リポジトリ** | 80% | ⚠️ 部分実装 | Playlist未実装 |
| **バックエンド - サービス** | 40% | ⚠️ 部分実装 | 一部のみ |
| **バックエンド - コントローラ** | 0% | ❌ 未着手 | **最優先課題** |
| **バックエンド - 例外処理** | 0% | ❌ 未着手 | - |
| **フロントエンド - UI** | 5% | ❌ 未着手 | 基本構成のみ |
| **フロントエンド - API連携** | 0% | ❌ 未着手 | - |
| **インフラ - Docker** | 100% | ✅ 完成 | - |
| **データ投入ツール** | 100% | ✅ 完成 | - |

### **総合完成度: 約 45%**

---

## 🎯 実装優先度

### 🔴 Critical（最優先 - 1-2週間）

#### 1. REST APIコントローラの実装

**現在の最大のボトルネック**

以下のコントローラを実装する必要があります:

**SongController.kt**
```kotlin
@RestController
@RequestMapping("/api/songs")
class SongController(private val songService: SongService) {
    @GetMapping
    fun getAllSongs(): ResponseEntity<List<Song>>
    
    @GetMapping("/{id}")
    fun getSong(@PathVariable id: String): ResponseEntity<Song>
    
    @PostMapping
    fun createSong(@RequestBody request: RegisterSongRequest): ResponseEntity<Song>
    
    @PutMapping("/{id}")
    fun updateSong(@PathVariable id: String, @RequestBody request: RegisterSongRequest): ResponseEntity<Song>
    
    @DeleteMapping("/{id}")
    fun deleteSong(@PathVariable id: String): ResponseEntity<Void>
}
```

**SingerController.kt**
```kotlin
@RestController
@RequestMapping("/api/singers")
class SingerController(private val singerService: SingerService)
```

**MusicSourceController.kt**
```kotlin
@RestController
@RequestMapping("/api/music-sources")
class MusicSourceController(private val musicSourceService: MusicSourceService)
```

**SearchController.kt**
```kotlin
@RestController
@RequestMapping("/api/search")
class SearchController(private val searchService: SearchService) {
    @GetMapping("/songs")
    fun searchSongsByTitle(@RequestParam title: String): ResponseEntity<List<Song>>
    
    @GetMapping("/artists")
    fun searchByArtist(@RequestParam artist: String): ResponseEntity<List<Song>>
    
    @GetMapping("/singers/{singerId}/songs")
    fun getSongsBySinger(@PathVariable singerId: String): ResponseEntity<List<Song>>
}
```

**必要な追加実装**:
- 適切なHTTPステータスコード返却
- 例外ハンドリング（@ControllerAdvice）
- リクエストバリデーション（@Valid）
- CORS設定

---

### 🟠 High（高優先度 - 2-3週間）

#### 2. 欠けているサービス層の実装

```kotlin
@Service
class SingerService(private val singerRepository: SingerRepository) {
    fun getAllSingers(): List<Singer>
    fun saveSinger(singer: Singer)
    fun deleteSinger(singerId: String)
}

@Service
class MusicSourceService(private val musicSourceRepository: MusicSourceRepository) {
    fun getAllMusicSources(): List<MusicSource>
    fun saveMusicSource(musicSource: MusicSource)
    fun deleteMusicSource(musicSourceId: String)
}

@Service
class PlaylistService(private val playlistRepository: PlaylistRepository) {
    // プレイリストCRUD
    // 楽曲の追加・削除・並び替え
}
```

#### 3. PlaylistRepositoryの完全実装

- プレイリストCRUD操作
- 楽曲の追加・削除
- 楽曲の順序変更（position更新）
- プレイリスト内の楽曲一覧取得

#### 4. フロントエンド基本機能

**必要なページ・コンポーネント**:
- `/pages/index.vue` - トップページ（楽曲一覧）
- `/pages/search.vue` - 検索ページ
- `/pages/playlists/[id].vue` - プレイリスト詳細
- `/components/SongCard.vue` - 楽曲カードコンポーネント
- `/components/MusicPlayer.vue` - プレーヤーコンポーネント
- `/composables/useApi.ts` - API通信用Composable

---

### 🟡 Medium（中優先度 - 1-2週間）

#### 5. エラーハンドリング・バリデーション

**カスタム例外クラス**:
```kotlin
// exception/
├── NotFoundException.kt
├── BadRequestException.kt
├── ConflictException.kt
└── GlobalExceptionHandler.kt (@RestControllerAdvice)
```

**バリデーション**:
- Bean Validation（@NotNull, @NotBlank等）
- カスタムバリデータ（時間範囲チェック等）

#### 6. API仕様書の整備

- Swagger/OpenAPIの導入
- エンドポイント仕様の文書化
- リクエスト/レスポンスサンプル

---

### 🟢 Low（低優先度 - 将来的な改善）

#### 7. 認証・認可

- Spring Security導入
- JWT認証
- ユーザー管理機能

#### 8. パフォーマンス最適化

- キャッシュ機構（Redis等）
- ページネーション実装
- レスポンス圧縮

#### 9. 監視・ロギング

- Spring Boot Actuator
- 構造化ログ
- メトリクス収集

---

## 💡 アーキテクチャ評価

### ✅ 優れている点

1. **クリーンアーキテクチャの採用**
   - ドメイン層とインフラ層の明確な分離
   - インターフェース（I*Repository）による依存性逆転原則の適用
   - テスタビリティの確保

2. **効率的なSQL設計**
   - CTE（WITH句）の積極的活用
   - JSON集約によるN+1問題の回避
   - UPSERTによる冪等性保証
   - 適切なインデックス戦略

3. **スケーラブルなインフラ**
   - コンテナベースの疎結合設計
   - 環境変数による設定外部化
   - 再現可能な開発環境

4. **型安全性**
   - KotlinとTypeScriptによる型安全な実装
   - Data Classによる不変性の保証

### ⚠️ 改善が必要な点

1. **APIレイヤーの欠如**
   - 外部公開エンドポイントが存在しない
   - フロントエンドとの統合が不可能
   - **プロジェクトの最大のボトルネック**

2. **エラーハンドリングの未実装**
   - 統一的な例外処理が未整備
   - バリデーション機構の欠如
   - エラーレスポンスの標準化が必要

3. **フロントエンドの未着手**
   - UIコンポーネントが皆無
   - API通信層の未実装
   - 状態管理の未設計

4. **テストカバレッジの不足**
   - リポジトリ層のみテスト存在
   - サービス層、コントローラ層のテストなし
   - E2Eテストの欠如

5. **ドキュメントの不足**
   - API仕様書なし
   - アーキテクチャドキュメントなし（本文書が最初）
   - セットアップガイドの不備

---

## 📈 今後の開発ロードマップ

### Phase 1: API公開（1-2週間）
- [ ] 全コントローラの実装
- [ ] DTOクラスの追加
- [ ] グローバル例外ハンドラ
- [ ] CORS設定
- [ ] API動作確認（Postman/curl）

### Phase 2: フロントエンド基礎（2-3週間）
- [ ] ページ構成の設計
- [ ] API通信Composableの実装
- [ ] 楽曲一覧ページ
- [ ] 検索機能UI
- [ ] 簡易プレーヤーコンポーネント

### Phase 3: プレイリスト機能（1-2週間）
- [ ] PlaylistRepository完全実装
- [ ] PlaylistService実装
- [ ] PlaylistController実装
- [ ] フロントエンド対応

### Phase 4: 品質向上（1-2週間）
- [ ] 統合テストの追加
- [ ] E2Eテストの実装
- [ ] エラーハンドリングの強化
- [ ] Swagger/OpenAPI導入

### Phase 5: 機能拡張（継続的）
- [ ] 認証・認可機能
- [ ] ユーザー管理
- [ ] お気に入り機能
- [ ] 再生履歴
- [ ] レコメンデーション

---

## 📝 結論

VMusicPlayerプロジェクトは、**堅実なデータベース設計と効率的なリポジトリ層実装**を基盤として、**約45%の完成度**に到達しています。

### 現状の最大の課題

**REST APIコントローラの完全欠如**により、現在はフロントエンドとの統合が一切できない状態です。これがプロジェクト進行の最大のボトルネックとなっています。

### 推奨される次のアクション

1. **最優先**: SongController、SearchControllerの実装
2. **次点**: 欠けているService層の補完
3. **並行**: フロントエンドのAPI通信層の準備

データベース設計とバックエンドの基礎部分は**非常に高品質**であるため、残りのレイヤーを着実に実装すれば、完成度の高いアプリケーションになる見込みです。

特に、以下の点が評価できます:
- ULID採用による分散システム対応
- pg_trgmによる高速検索基盤
- JSON集約によるパフォーマンス最適化
- クリーンアーキテクチャによる保守性

APIレイヤーの実装完了後は、フロントエンド開発とのパラレル作業が可能になり、プロジェクト全体の進捗が大きく加速すると予想されます。

---

**ドキュメント更新**: このファイルは実装の進捗に応じて定期的に更新してください。

