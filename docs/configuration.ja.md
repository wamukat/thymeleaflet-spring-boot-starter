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
| `thymeleaflet.resources.cache-duration-seconds` | int | `3600` | キャッシュ秒数 |

`resources.stylesheets` は **Shadow DOM のプレビュー領域**にのみ注入されます。
レイアウトやテーマの再現は [stories.ja.md](stories.ja.md) の `preview.wrapper` を使ってください。
サンプルでは `/css/mypage.css` と `/css/mypage/components.css` を注入し、
アプリと同じ見た目になるよう揃えています。

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

## 内部設定（マイグレーション）

| プロパティ | 型 | デフォルト | 説明 |
|---|---|---|---|
| `thymeleaflet.migration.phase` | String | `"4.0"` | 内部フェーズ管理 |
| `thymeleaflet.migration.monitoring.response-time-degradation-threshold` | int | `10` | 内部監視設定 |
| `thymeleaflet.migration.monitoring.error-rate-increase-threshold` | int | `1` | 内部監視設定 |
| `thymeleaflet.migration.monitoring.enforce-contract-tests` | boolean | `true` | 内部監視設定 |

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
    cache-duration-seconds: 3600
  security:
    enabled: true
```
