# HTML 内 JavaDoc コメント

Thymeleaflet は HTML テンプレート内の JavaDoc 風コメントを解析し、
フラグメントの説明・パラメータ・使用例を抽出します。任意ですが推奨です。

## 基本形式

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

## タグ

- `@param name {@code Type} [required|optional=default] 説明`
- `@model name {@code Type} [required|optional=default] 説明`
- `@example <div th:replace="~{path :: fragment(...)}"></div>`
- `@background` はプレビュー背景指定

## 補足

- `@example` がフラグメントの紐付けに使われます。
- 型と必須/任意の記載を推奨します。
- コメントは HTML コメント内（`<!-- ... -->`）に置いてください。
