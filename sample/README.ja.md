# Thymeleaflet サンプル

Thymeleaflet + DaisyUI のサンプルアプリです。

## セットアップ（利用者向け）

1) スターター依存を追加（Maven Central から取得）:

```xml
<dependency>
  <groupId>io.github.wamukat</groupId>
  <artifactId>thymeleaflet-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

2) サンプルCSS（DaisyUI + Tailwind）をビルド（このディレクトリで実行）:

```bash
npm install
npm run build
```

3) サンプルアプリを起動（このディレクトリで実行）:

```bash
mvn spring-boot:run
```

4) ブラウザで確認:

```
http://localhost:6006/
```

フォームサンプル:

```
http://localhost:6006/forms
```

## セットアップ（開発者向け）

スターターの最新コードをローカルで試す場合:

1) スターターをローカルMavenにインストール（リポジトリのルートで実行）:

```bash
./mvnw -DskipTests install
```

2) サンプルCSS（DaisyUI + Tailwind）をビルド（このディレクトリで実行）:

```bash
npm install
npm run build
```

3) サンプルアプリを起動（このディレクトリで実行）:

```bash
mvn spring-boot:run
```

## メモ

- `/` のページは Thymeleaflet でプレビューできるフラグメントを使用しています。
- フラグメントプレビュー:
  - `http://localhost:6006/thymeleaflet`
- 生成されたCSSは以下に出力されます:
  - `src/main/resources/static/css/mypage.css`
  - `src/main/resources/static/css/mypage/components.css`
- ストーリーファイルの配置場所:
  - `src/main/resources/META-INF/thymeleaflet/stories/`
- Thymeleaflet は開発者向けツールです。本番では
  `spring.autoconfigure.exclude` を使って無効化してください
  （`src/main/resources/application-prod.yml` を参照）

## スクリーンショット用データ

ダミーデータは以下にあります:

- `templates/components/profile-card.html`
- `META-INF/thymeleaflet/stories/components/profile-card.stories.yml`
