# insert.py - データ取り込みツール実行方法

## 📋 概要

このスクリプトは、JSON形式の楽曲データをPostgreSQLデータベースに取り込むためのツールです。

## 🔧 必要な環境

### Pythonバージョン
- Python 3.10以上推奨

### 必要なパッケージ
```bash
pip install psycopg python-dateutil ulid-py
```

または、requirements.txtを作成する場合：
```txt
psycopg>=3.0.0
python-dateutil>=2.8.0
ulid-py>=1.1.0
```

## 🚀 実行方法

### 基本的な使い方

```bash
python insert.py --json <JSONファイルのパス>
```

### 例：既存のJSONファイルを取り込む

```bash
# MementoVのデータを取り込む
python insert.py --json MementoV_music_sources.json

# Tengoku Amaneのデータを取り込む
python insert.py --json Tengoku_Amane_sources.json

# Tsukasaのデータを取り込む
python insert.py --json Tsukasa_music_sources.json
```

### データベース接続オプション

デフォルトでは以下の設定が使用されます：
- ホスト: `localhost`
- ポート: `5432`
- ユーザー: `VMusicPlayer`
- パスワード: `VMusicPlayer`
- データベース: `VMusicPlayer`

これらを変更する場合：

```bash
python insert.py \
  --json MementoV_music_sources.json \
  --host localhost \
  --port 5432 \
  --user VMusicPlayer \
  --password VMusicPlayer \
  --dbname VMusicPlayer
```

### SSL接続を使用する場合

```bash
python insert.py --json データ.json --sslmode require
```

SSL モード:
- `disable` - SSL無効
- `require` - SSL必須
- `verify-ca` - CA証明書検証
- `verify-full` - 完全検証

## 📄 JSONファイルの形式

```json
[
  {
    "title": "配信タイトル",
    "url": "https://www.youtube.com/watch?v=VIDEO_ID",
    "upload_date": "2025/07/09",
    "thumbnail_url": "http://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg",
    "sourceType": "live",
    "songs": [
      {
        "title": "曲名",
        "start_at": "5:46",
        "end_at": "9:57",
        "singers": ["歌手名1", "歌手名2"],
        "artist": "アーティスト名"
      }
    ]
  }
]
```

### フィールド説明

#### ソース情報
- `title`: 配信/動画のタイトル（必須）
- `url`: YouTube URL（必須、ユニークキー）
- `upload_date`: アップロード日（形式: YYYY/MM/DD）
- `thumbnail_url`: サムネイルURL
- `sourceType`: ソースタイプ（`live`, `youtube_video` など）

#### 楽曲情報
- `title`: 楽曲名（必須）
- `start_at`: 開始時刻（必須、形式: `m:ss` または `h:mm:ss`）
- `end_at`: 終了時刻（必須、形式: `m:ss` または `h:mm:ss`）
- `singers`: 歌手名の配列
- `artist`: 原曲アーティスト名

### 時刻の形式

以下の形式をサポート：
- `5:46` → 5分46秒
- `1:30:00` → 1時間30分
- `90` → 90秒

## 🔄 冪等性（再実行時の動作）

このスクリプトは**ほぼ冪等**です：

✅ **重複しないもの**：
- 同じURLのmusic_source
- 同じ名前のsinger
- 同じ(source_id, title, start_at)のsong

⚠️ **更新されるもの**：
- 既存の楽曲の`end_at`（終了時刻）

⚠️ **注意**：
- 開始時刻を変更すると別の楽曲として追加されます
- ソースのタイトルやサムネイルは更新されません

## 📝 実行例

### ステップ1: データベースが起動していることを確認

```bash
# Docker Composeを使用している場合
cd E:\VMusicPlayer
docker-compose up -d
```

### ステップ2: Pythonパッケージをインストール

```bash
cd E:\VMusicPlayer\InsertTools
pip install psycopg python-dateutil ulid-py
```

### ステップ3: データを取り込む

```bash
# 1つ目のファイル
python insert.py --json MementoV_music_sources.json

# 2つ目のファイル
python insert.py --json Tengoku_Amane_sources.json

# 3つ目のファイル
python insert.py --json Tsukasa_music_sources.json
```

成功すると以下のメッセージが表示されます：
```
Ingest completed successfully.
```

## 🐛 トラブルシューティング

### エラー: `ModuleNotFoundError: No module named 'psycopg'`
```bash
pip install psycopg
```

### エラー: `connection refused`
- PostgreSQLが起動していることを確認
- ホスト・ポート設定を確認
- Docker Composeの場合: `docker-compose ps` で状態確認

### エラー: `authentication failed`
- ユーザー名・パスワードを確認
- `--user` と `--password` オプションで明示的に指定

### エラー: `database "VMusicPlayer" does not exist`
- データベースが作成されていることを確認
- スキーマ初期化: `psql -U VMusicPlayer -d VMusicPlayer -f ../db/init/01_schema.sql`

## 📊 取り込み後の確認

```bash
# データベースに接続
psql -U VMusicPlayer -d VMusicPlayer

# 取り込まれたデータを確認
SELECT COUNT(*) FROM music_sources;
SELECT COUNT(*) FROM songs;
SELECT COUNT(*) FROM singers;

# 楽曲一覧を表示
SELECT s.title, ms.title as source_title 
FROM songs s 
JOIN music_sources ms ON s.source_id = ms.id 
LIMIT 10;
```

または、フロントエンドで確認：
1. バックエンドを起動: `cd ../VMusicPlayerServer && ./gradlew bootRun`
2. フロントエンドを起動: `cd ../VMusicPlayerApp && npm run dev`
3. ブラウザで http://localhost:3000 にアクセス

## 📚 関連ファイル

- `insert.py` - メインスクリプト
- `MementoV_music_sources.json` - メーメントヴァニタスのデータ
- `Tengoku_Amane_sources.json` - 天国あまねのデータ
- `Tsukasa_music_sources.json` - ツカサのデータ
- `../db/init/01_schema.sql` - データベーススキーマ

## 🔗 参考

- [psycopg3ドキュメント](https://www.psycopg.org/psycopg3/)
- [python-dateutilドキュメント](https://dateutil.readthedocs.io/)
- [ULID仕様](https://github.com/ulid/spec)

