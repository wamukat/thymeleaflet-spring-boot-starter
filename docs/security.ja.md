# セキュリティ

Thymeleaflet は `/thymeleaflet/**` 用のセキュリティフィルターチェーンを内蔵しています。
デフォルトでは有効です。
開発用途向けの補助ツールとして利用してください。

## 設定

```yaml
thymeleaflet:
  security:
    enabled: true
```

## デフォルト挙動

- `/thymeleaflet/**` は許可 (開発向け)
- CSRF は Cookie ベースで有効
- HTMX プレビュー/コンテンツなどは CSRF 除外
- セキュリティヘッダー (HSTS/CSP/Referrer-Policy/Permissions-Policy) を付与

## 推奨

- 本番では無効化、またはアクセス制限を推奨
- 企業内・限定 IP での運用を想定
- 独自セキュリティを使う場合は無効化して独自チェーンを定義

本番では自動設定を除外する方法がシンプルです:

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```
