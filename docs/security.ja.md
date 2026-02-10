# セキュリティ

Thymeleaflet は Spring Security のフィルターチェーンを自動登録しません。
開発用途向けの補助ツールとして利用してください。

## 連携方針

セキュリティ挙動は利用側アプリで管理します。
Spring Security を使う場合は、Opt-in 自動許可か明示設定を選択できます。

### Option A: Opt-in 自動許可（手早く使う）

```yaml
thymeleaflet:
  security:
    auto-permit: true
```

この設定で `/thymeleaflet/**` のみを許可する最小チェーンを登録します。

### Option B: 利用側で明示設定

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/thymeleaflet/**").permitAll()
    .anyRequest().authenticated()
);
```

## 挙動

- Thymeleaflet 自体は認可/認証ルールを追加しません。
- Thymeleaflet 自体は CSRF/ヘッダー/セッション制御を追加しません。
- 既存アプリのセキュリティ設定がそのまま有効です。
- `auto-permit=true` の場合のみ、`/thymeleaflet/**` 向けの最小許可チェーンを追加します。

## 推奨

- 本番では `/thymeleaflet/**` へのアクセス制限を推奨
- 企業内・限定 IP での運用を想定

本番では自動設定を除外する方法がシンプルです:

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## 関連

- 設定: [configuration.ja.md](configuration.ja.md)
