# MemoApp Repository & Design Guidelines

このドキュメントは、`memoApp` リポジトリの保守、機能拡張、およびデザイン変更を行う際の一貫性を保つための「指標（ガイドライン）」です。

---

## 1. アーキテクチャ原則 (Repository Architecture Principles)

### ① Local-First（ローカルファースト設計）
* **方針**: クラウド（Firebase Firestore）の同期に依存せず、Android 端末内のプライベートストレージ（`context.filesDir`）のローカルフォルダ構造を主軸としてデータを自立保存・参照できるようにします。
* **実装**: `ConceptLocalRepository`, `NoteLocalRepository`, `MeasurementLocalRepository`, `SymbolLocalRepository`, `LogItemRepository` を通じて、JSON / Markdown ファイルとしてローカルに永続化します。

### ② クラウドとの完全な疎結合
* **方針**: クラウド接続や同期処理（Firebase）はオプションのレイヤーとして分離し、オフライン環境でもアプリの全機能（ローカル保存・キャンバス・測定・TODO管理）が完全動作するように保ちます。

---

## 2. デザイン・UI/UX 指針 (Design System Guidelines)

### ① Material 3 & Jetpack Compose の統一
* **方針**: UIコンポーネントは可能な限り Material 3 (`androidx.compose.material3`) を採用し、プロジェクト全体のルック＆フェールを統一します。
* **キャンバス設計**: 複雑なカスタム `Canvas` 描画よりも、DOM風絶対配置（`Box` + `Modifier.offset` + 個別 Compose カード）を採用し、テキストのはみ出しやタップ判定のズレを防ぎます。

### ② モーダル・ダイアログの標準化
* **方針**: アプリ内のすべての `AlertDialog` モーダルは、拡張関数 `makeFullScreen()` を使用して**標準で全画面（`MATCH_PARENT`）**で展開されるように統一します。
* **ナビゲーション**: 記事やノートの詳細・編集画面は、ポップアップモーダルではなく専用のフルスクリーンエディタ Fragment（例: `NoteFragment`, `LogItemFragment`）として一貫させます。

### ③ ナビゲーション補助 (Minimap & Scrollbar)
* **方針**: 長文エディタや詳細ビューには、サイドの縦型スクロールバーと連動した「2段階ジャンプモーダル（`ScrollbarWithJumpModal`）」や「ラインベースのチェックリスト（`ChecklistTextView`）」を配置し、操作性を高めます。

---

## 3. コーディング規約・ベストプラクティス (Coding Standards)

* **Kotlin ＆ Coroutines**: 非同期処理や状態管理には Kotlin Coroutines および `StateFlow` / `collectAsStateWithLifecycle` を使用します。
* **堅牢なエラーハンドリング**: ローカルファイル（JSON/Markdown）のパース時には必ず `try-catch` や `runCatching` を用い、古いキャッシュスキーマによるアプリクラッシュ（`JsonSyntaxException` 等）を確実に防ぎます。
* **画面常時点灯（Keep Screen On）**: 常時確認が必要なTODOやフローティング小窓では `FLAG_KEEP_SCREEN_ON` を適用し、利便性を向上させます。
