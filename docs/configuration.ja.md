# 設定

全ての設定は `thymeleaflet` プレフィックス配下です。
使い方の詳細は [getting-started.ja.md](getting-started.ja.md) と
[stories.ja.md](stories.ja.md) を参照してください。

## 基本プロパティ

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.base-path` | String | `/thymeleaflet` | UI のベースパス |
| `thymeleaflet.debug` | boolean | `false` | フラグメント探索のデバッグログ |

## リソース設定

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.resources.template-paths` | List<String> | [`/templates/`] | テンプレート探索パス (1〜5件) |
| `thymeleaflet.resources.stylesheets` | List<String> | `[]` | プレビューに注入する CSS (最大10件) |
| `thymeleaflet.resources.scripts` | List<String> | `[]` | プレビューiframeに注入する JS (最大10件) |
| `thymeleaflet.resources.cache-duration-seconds` | int | `3600` | キャッシュ秒数 |

`resources.stylesheets` と `resources.scripts` は **iframeプレビュー**にのみ注入されます。
レイアウトやテーマの再現は [stories.ja.md](stories.ja.md) の `preview.wrapper` を使ってください。
サンプルでは `/css/mypage.css` と `/css/mypage/components.css` を注入し、
アプリと同じ見た目になるよう揃えています。
JavaScript を使いたい場合は `resources.scripts` に登録してください。
例: `<div data-theme=\"light\">{{content}}</div>`
プレビュー iframe は same-origin を許可しているため、Cookie / localStorage / 認証付きAPIが動作します。

## プレビュー設定

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.preview.background-light` | String | `#f3f4f6` | プレビューの明るい背景色 |
| `thymeleaflet.preview.background-dark` | String | `#1f2937` | プレビューの暗い背景色 |
| `thymeleaflet.preview.viewports` | List | 組み込みプリセット | ビューポート一覧（名前＋幅、Fitは除外） |

ビューポート一覧はドロップダウンに表示されます。Fit は常に利用可能で、この一覧には含めません。
各項目は `id` / `label` / `width` を持ちます。

## キャッシュ設定

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.cache.enabled` | boolean | `true` | フラグメント探索・JavaDoc解析・依存解析のメモリキャッシュ |
| `thymeleaflet.cache.preload` | boolean | `false` | 起動時にキャッシュをウォームアップ |

### CSP 補足（意図的に緩め）

Thymeleaflet はプレビューで外部 JS/CSS を使えるよう、CSP を意図的に緩めています。
利便性は上がりますが **保護は弱くなります**。必ず認証された環境でのみ利用してください。

## セキュリティ

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.security.enabled` | boolean | `true` | Thymeleaflet 用セキュリティの有効化 |

## 環境ごとの有効/無効

Thymeleaflet は開発中の補助ツールとして利用する想定です。本番環境では
自動設定を除外するか、本番ビルドから依存関係を外してください。

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## 設定例

```yaml
thymeleaflet:
  base-path: /thymeleaflet
  debug: false
  resources:
    template-paths:
      - /templates/
    stylesheets:
      - /css/app.css
    scripts:
      - /js/app.js
    cache-duration-seconds: 3600
  preview:
    background-light: "#f7f7f9"
    background-dark: "#1f2937"
    viewports:
      - id: mobileSmall
        label: Mobile Small
        width: 320
      - id: tablet
        label: Tablet
        width: 834
  cache:
    enabled: true
    preload: false
  security:
    enabled: true
```
