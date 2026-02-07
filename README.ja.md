# Thymeleaflet Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.wamukat/thymeleaflet-spring-boot-starter.svg)](https://central.sonatype.com/artifact/io.github.wamukat/thymeleaflet-spring-boot-starter)

<p align="center">
  <img src="assets/logo/thymeleaflet-logo.png" width="320" alt="Thymeleaflet ロゴ">
</p>

Thymeleaflet は、Thymeleaf フラグメント向けの軽量 Storybook UI を提供します。
HTML テンプレート内の JavaDoc 風コメントを解析して、プレビューや使用例を生成します。

- フラグメント一覧とプレビュー UI
- JavaDoc 解析（パラメータ/モデル/使用例）
- YAML によるストーリー定義
- Tailwind ベースの UI スタイル

デモサイト: https://bad-siouxie-wamukat-977c4dc6.koyeb.app/thymeleaflet

## 位置づけ（開発者向けツール）

Thymeleaflet は開発中の確認に使う補助ツールです。本番環境への公開は想定していません。

推奨構成（dev で有効化し、prod で無効化）:

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

本番ビルドから依存関係を外す運用も推奨します。

## 必要要件

- Java 17 以上
- Spring Boot 3.1 以上
- Maven または Gradle

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

1) 依存を追加して Spring Boot アプリを起動します。
2) デフォルトの UI にアクセス:

```
http://localhost:6006/thymeleaflet
```

サンプルはポート競合を避けるため `6006` を使っています。

3) フラグメントテンプレートを通常の Thymeleaf テンプレート配下に配置します。
4) （任意）ストーリー定義は以下に配置します:

```
META-INF/thymeleaflet/stories/{templatePath}.stories.yml
```

## 設定

```yaml
thymeleaflet:
  base-path: /thymeleaflet
  debug: false
  resources:
    template-paths:
      - /templates/
    stylesheets: []
    scripts: []
    cache-duration-seconds: 3600
  cache:
    enabled: true
    preload: false
  security:
    enabled: true
```

### 補足

- `thymeleaflet.base-path` で UI のパスを変更できます。
- `resources.template-paths` は 1〜5 件の指定が必要です。
- `resources.stylesheets` は最大 10 件まで指定できます。
- `resources.scripts` は最大 10 件まで指定できます（プレビューiframe内に注入）。
- JS を使う場合は `resources.scripts` に登録し、`preview.wrapper` で必要なDOM構造を整えてください。
  - 例: `<div data-theme=\"light\">{{content}}</div>`
- `cache.enabled` はフラグメント探索・JavaDoc解析・依存解析のメモリキャッシュを有効化します。
- `cache.preload` は起動時にキャッシュをウォームアップします（低CPU環境向け）。
- CSP はプレビューで外部 JS/CSS を使えるよう意図的に緩めています。信頼できる環境でのみ利用してください。
- プレビュー iframe は same-origin を許可しているため、Cookie / localStorage / 認証付きAPIが動作します。
- `security.enabled` で `/thymeleaflet/**` のセキュリティ設定を切り替えます。

## エンドポイント

- `{basePath}`: フラグメント一覧 UI
- `{basePath}/main-content`: 遅延読み込み用のメインコンテンツ
- `{basePath}/{templatePath}/{fragmentName}/{storyName}`: ストーリープレビュー
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/content`: HTMX 用コンテンツ
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/render`: 動的レンダリング
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/usage`: 使用例

## HTML 内 JavaDoc コメント

```html
<!--
/**
 * 会員情報詳細表示（memberDetail）
 *
 * @param variant {@code String} [optional=standard] 表示バリアント
 * @model memberProfile {@code List<Map<String, Object>>} [required] 会員情報モデル
 * @example <div th:replace="~{domain/member/organisms/member-profile :: memberDetail()}"></div>
 * @background gray-50
 */
-->
```

## フラグメントシグネチャ解析ポリシー

- 基準: Thymeleaf `3.1.2.RELEASE`
- 参照パーサ: `org.thymeleaf.standard.expression.FragmentSignatureUtils`
- v1 では、UIで安定サポートする範囲を Thymeleaf の受理範囲より狭く定義しています。

現時点での安定サポート:

- `th:fragment="name"`
- `th:fragment="name()"`
- `th:fragment="name(param1, param2)"`

補足:

- 非対応のシグネチャは発見対象からスキップし、診断情報を出力します。
- 診断は code/severity をログ出力し、UIには安全なメッセージのみ表示します。
- 詳細は `docs/parser-spec.ja.md` / `docs/parser-model.ja.md` を参照してください。

## ローカルビルド

```bash
./mvnw test
npm install
npm run build
```

## ドキュメント

以下を参照してください。

- [docs/README.ja.md](docs/README.ja.md)
- [docs/getting-started.ja.md](docs/getting-started.ja.md)
- [docs/configuration.ja.md](docs/configuration.ja.md)
- [docs/javadoc.ja.md](docs/javadoc.ja.md)
- [docs/stories.ja.md](docs/stories.ja.md)
- [docs/security.ja.md](docs/security.ja.md)
- [docs/parser-spec.ja.md](docs/parser-spec.ja.md)
- [docs/parser-model.ja.md](docs/parser-model.ja.md)

## コントリビューション

[CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。

## セキュリティ

[SECURITY.md](SECURITY.md) を参照してください。

## ライセンス

Apache-2.0. [LICENSE](LICENSE) を参照してください。

## English

English README: [README.md](README.md)
