# VMusicPlayer - 曲一覧表示機能

## 実装内容

SongControllerから曲の一覧を取得して表示する機能を実装しました。

### 実装されたファイル

1. **types/song.ts** - 型定義
   - Song, MusicSource, Singer のTypeScriptインターフェース

2. **components/SongList.vue** - 曲一覧表示コンポーネント
   - APIから曲一覧を取得
   - カード形式で曲情報を表示
   - サムネイル画像表示
   - 歌手タグ表示
   - 再生時間の表示

3. **config/WebConfig.kt** - CORS設定
   - フロントエンドからのAPIアクセスを許可

4. **nuxt.config.ts** - API設定
   - バックエンドAPIのベースURL設定

### 起動方法

#### バックエンド (Spring Boot)
```bash
cd VMusicPlayerServer
./gradlew bootRun
```

#### フロントエンド (Nuxt)
```bash
cd VMusicPlayerApp
npm install  # 初回のみ
npm run dev
```

### アクセス方法

1. バックエンドAPI: http://localhost:8080
2. フロントエンド: http://localhost:3000

ブラウザで http://localhost:3000 にアクセスすると、曲の一覧が表示されます。

### API エンドポイント

- `GET /api/songs` - 全曲取得

### 表示される情報

- 曲のタイトル
- アーティスト名
- 歌手一覧（タグ形式）
- ソース動画のサムネイル
- ソース動画へのリンク
- 再生時間（開始〜終了）

### 注意事項

- TypeScriptのエラーが表示される場合がありますが、Nuxtの自動インポート機能により実行時には問題なく動作します
- 開発サーバー起動後、`.nuxt`ディレクトリが生成され、型定義が自動的に解決されます
- データベースが起動していることを確認してください（PostgreSQL）

### 環境変数

フロントエンドでAPIのベースURLを変更する場合：

```bash
# .env ファイルを作成
API_BASE_URL=http://localhost:8080
```

