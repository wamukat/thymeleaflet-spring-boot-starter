# セキュリティ

Spring Security が存在する場合、Thymeleaflet はデフォルトで `/thymeleaflet/**` を許可する最小限のフィルターチェーンを登録します。
開発用途向けの補助ツールとして利用してください。

## 連携方針

セキュリティ挙動は利用側アプリで管理します。
Spring Security を使う場合は、デフォルトの自動許可を使うか、opt out して明示設定できます。

### Option A: デフォルトの自動許可（手早く使う）

```yaml
thymeleaflet:
  security:
    auto-permit: true
```

この設定で `/thymeleaflet/**` のみを許可する最小チェーンを登録します。
ローカル開発または信頼できる内部環境向けの quick start として扱ってください。
`prod` または `production` profile で `auto-permit` が有効な場合、起動時に WARN を出します。
この補助設定は、明示的に `false` を指定しない限りデフォルトで有効です。

### Option B: Opt out して利用側で明示設定

```yaml
thymeleaflet:
  security:
    auto-permit: false
```

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/thymeleaflet/**").permitAll()
    .anyRequest().authenticated()
);
```

## 挙動

- 自動許可ヘルパーは `/thymeleaflet/**` の認可だけを追加します。
- 自動許可ヘルパーは、Custom story の POST レンダリングが動作するよう Thymeleaflet UI パスの CSRF を無効化します。
- Thymeleaflet はヘッダー/セッション制御を追加しません。
- 既存アプリのセキュリティ設定がそのまま有効です。
- `auto-permit=false` の場合、Thymeleaflet は `SecurityFilterChain` を追加しません。

## 推奨

- 本番では `/thymeleaflet/**` へのアクセス制限を推奨
- 企業内・限定 IP での運用を想定

本番では自動設定を除外する方法がシンプルです:

```yaml
# application-prod.yml
thymeleaflet:
  enabled: false
```

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## 関連

- 設定: [configuration.ja.md](configuration.ja.md)
