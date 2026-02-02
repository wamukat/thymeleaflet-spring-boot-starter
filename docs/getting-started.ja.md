# はじめに

## 必要要件

- Java 17 以上
- Spring Boot 3.1 以上

## 導入

Maven:

```xml
<dependency>
  <groupId>io.github.wamukat</groupId>
  <artifactId>thymeleaflet-spring-boot-starter</artifactId>
  <version>0.0.1</version>
</dependency>
```

Gradle:

```kotlin
dependencies {
    implementation("io.github.wamukat:thymeleaflet-spring-boot-starter:0.0.1")
}
```

## クイックスタート

1) 依存を追加します。
2) Spring Boot アプリを起動します。
3) UI にアクセスします。

```
http://localhost:6006/thymeleaflet
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

形式は `stories.ja.md` を参照してください。

## 次に読む

- 設定: `configuration.ja.md`
- JavaDoc: `javadoc.ja.md`
- ストーリー/プレビュー: `stories.ja.md`
