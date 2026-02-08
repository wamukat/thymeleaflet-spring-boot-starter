# はじめに

## 必要要件

- Java 17 以上（実行・ライブラリ利用時）
- Java 21 以上（このリポジトリをソースからビルドする場合）
- Spring Boot 3.1 以上

## 導入

Maven:

```xml
<dependency>
  <groupId>io.github.wamukat</groupId>
  <artifactId>thymeleaflet-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
dependencies {
    implementation("io.github.wamukat:thymeleaflet-spring-boot-starter:0.1.0")
}
```

## クイックスタート

1) 依存を追加します。
2) Spring Boot アプリを起動します。
3) UI にアクセスします。

```
http://localhost:6006/thymeleaflet
```

サンプルはポート競合を避けるため `6006` を使っています。
必要であれば `server.port` で変更できます。

## サンプルページ

- アプリ画面: `http://localhost:6006/`
- フォーム画面: `http://localhost:6006/forms`
- Thymeleaflet UI: `http://localhost:6006/thymeleaflet`

### UI 画面構成（参照用）

```
[Global Header]
  - ロゴ / タイトル
  - ページツール（言語切替など）

[Main Layout]
  ├─ [Left Sidebar] Fragment List
  │    ├─ [Fragment Search]
  │    └─ [Fragment Tree/List]
  │
  ├─ [Center Pane] Story Panel
  │    ├─ [Story List]
  │    └─ [Story Values]
  │         ├─ [Custom Parameters]
  │         └─ [Custom Model]
  │
  └─ [Right Pane] Preview & Details
       ├─ [Preview Panel]
       │    ├─ [Preview Toolbar]
       │    └─ [Preview Canvas] (iframe)
       ├─ [Usage Panel]
       └─ [Fragment Details Panel]
```

## サンプルのスタイル（Tailwind + daisyUI）

サンプルは Tailwind CSS と daisyUI を使用しています。起動前に CSS をビルドしてください。

```bash
npm install
npm run build
```

## 開発用途での利用

Thymeleaflet は開発中の補助ツールとしての利用を想定しています。
本番公開は避け、dev プロファイルで有効化し prod で無効化してください:

```yaml
# application-dev.yml
thymeleaflet:
  base-path: /thymeleaflet
  security:
    enabled: false

# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

本番ビルドから依存関係を外す運用でも問題ありません。

## 代表的な配置場所

- テンプレート: `src/main/resources/templates/**`
- ストーリー: `src/main/resources/META-INF/thymeleaflet/stories/**`

## 最初のフラグメントとストーリー

1) テンプレートにフラグメントを作成します。
2) 同じテンプレートパスに対応するストーリー YAML を作成します。
3) UI をリロードしてフラグメントを選択します。

形式は [stories.ja.md](stories.ja.md) を参照してください。

## 次に読む

- 設定: [configuration.ja.md](configuration.ja.md)
- JavaDoc: [javadoc.ja.md](javadoc.ja.md)
- ストーリー/プレビュー: [stories.ja.md](stories.ja.md)
