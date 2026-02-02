# Thymeleaflet Spring Boot Starter

Thymeleaflet は、Thymeleaf フラグメント向けの軽量 Storybook UI を提供します。
HTML テンプレート内の JavaDoc 風コメントを解析して、プレビューや使用例を生成します。

- フラグメント一覧とプレビュー UI
- JavaDoc 解析（パラメータ/モデル/使用例）
- YAML によるストーリー定義
- Tailwind ベースの UI スタイル

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
  <version>RELEASE_VERSION</version>
</dependency>
```

Gradle:

```kotlin
dependencies {
    implementation("io.github.wamukat:thymeleaflet-spring-boot-starter:RELEASE_VERSION")
}
```

## クイックスタート

1) 依存を追加して Spring Boot アプリを起動します。
2) デフォルトの UI にアクセス:

```
http://localhost:6006/thymeleaflet
```

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
    cache-duration-seconds: 3600
  security:
    enabled: true
  migration:
    phase: "4.0"
    monitoring:
      response-time-degradation-threshold: 10
      error-rate-increase-threshold: 1
      enforce-contract-tests: true
```

### 補足

- `thymeleaflet.base-path` で UI のパスを変更できます。
- `resources.template-paths` は 1〜5 件の指定が必要です。
- `resources.stylesheets` は最大 10 件まで指定できます。
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
 * @model memberProfile {@code MemberProfile} [required] 会員情報モデル
 * @example <div th:replace="~{domain/member/organisms/member-profile :: memberDetail()}"></div>
 * @background gray-50
 */
-->
```

## ローカルビルド

```bash
./mvnw test
npm install
npm run build
```

## ドキュメント

以下を参照してください。

- `docs/getting-started.md`
- `docs/configuration.md`
- `docs/javadoc.md`
- `docs/stories.md`
- `docs/security.md`

## コントリビューション

`CONTRIBUTING.md` を参照してください。

## セキュリティ

`SECURITY.md` を参照してください。

## ライセンス

Apache-2.0. `LICENSE` を参照してください。
