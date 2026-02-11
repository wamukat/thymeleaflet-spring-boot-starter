# ストーリー (YAML)

ストーリーは以下から読み込まれます。

```
META-INF/thymeleaflet/stories/{templatePath}.stories.yml
```

## 例

```yaml
meta:
  title: Buttons
  description: DaisyUI button variations

storyGroups:
  primaryButton:
    title: Primary Button
    description: Primary button variants
    stories:
      - name: default
        title: Default
        parameters:
          label: Save
          variant: primary
      - name: outline
        title: Outline
        parameters:
          label: Outline
          variant: outline
```

## スキーマ

- `meta`
  - `title`: 全体タイトル
  - `description`: 全体説明
- `storyGroups`
  - キー: フラグメント名
  - `title`, `description`
  - `stories`: ストーリー配列

### ストーリー項目

- `name`: ストーリー名 (URL に使用)
- `title`: 表示名
- `description`: 任意
- `parameters`: フラグメント引数
- `model`: モデル値
- `preview.wrapper`: プレビューのラッパー

## Model（モデル）

フラグメントが **オブジェクトを参照**する場合は `model` を使います。
`model` の内容は Thymeleaf のモデルにマージされてからレンダリングされます。

### 例

フラグメント例（`profile` を参照）:

```html
<div th:fragment="profileCard()">
  <h2 th:text="${profile.name}">Name</h2>
  <p th:text="${profile.role}">Role</p>
</div>
```

ストーリー例:

```yaml
stories:
  - name: default
    title: Default
    model:
      profile:
        name: Alex Morgan
        role: Product Designer
        plan: Pro
        region: APAC
        points: 1280
        projects: 8
```

### 補足

- `model` はネストした Map / List を扱えます。
- `model` と `parameters` は同一ストーリーで併用できます。
- キーが不足すると、Thymeleaf の式は `null` になる可能性があります。

## プレビューラッパー

`preview.wrapper` でアプリ本体のレイアウトやテーマを再現できます。
iframe プレビュー内で描画されるため、`{{content}}` を必ず含めてください。
サンプルのストーリーでは daisyUI の見た目を合わせるために wrapper を使っています。

```yaml
preview:
  wrapper: |
    <div data-theme="light" class="bg-base-200 px-6 py-6 text-base-content font-display">
      <div class="max-w-6xl mx-auto">
        {{content}}
      </div>
    </div>
```

## Custom ストーリー（UI 上書き）

Thymeleaflet は UI に **Custom** ストーリーを追加し、パラメータを編集できます。

- **初期値**: `default` があればその値、なければ先頭ストーリーの値をコピー。
- **保持先**: `sessionStorage` にフラグメント単位で保存。
- **編集対象**: `parameters` と `model` の両方。
- **適用範囲**: プレビュー表示のみ（`stories.yml` には書き戻しません）。
- **予約名**: `custom` は UI で使用するため、stories.yml には定義しないでください。

## URL の挙動

- URL: `/thymeleaflet/{templatePath}/{fragmentName}/{storyName}`
- `/default` が指定されたが `default` が存在しない場合、ファイルの**先頭ストーリー**へリダイレクトします。

## フラグメント参照パラメータを安全に扱う

`th:replace` / `th:insert` に渡す値は、`~{...}` のフラグメント式にしてください。

NG 例:

```html
<th:block th:replace="${body}"></th:block>
```

なぜ危険か:

- `body: value` のような通常文字列が来ると、Thymeleaf がテンプレート名として解決しにいき、`TemplateInputException` になることがあります。

推奨パターン（安全な分岐）:

```html
<th:block th:if="${body != null and #strings.startsWith(body.toString(), '~')}">
  <th:block th:replace="~{${body}}"></th:block>
</th:block>
<p th:if="${body != null and !#strings.startsWith(body.toString(), '~')}" th:text="${body}"></p>
```

Story パラメータ設計ガイド:

- `th:replace` / `th:insert` 用パラメータは `~{template :: fragment(...)}` を必須にする。
- `value` のような汎用ダミー値を `th:replace` に直接渡さない。
- 文字列パラメータとフラグメント参照パラメータを命名で分離する（例: `bodyText` / `bodyFragment`）。

## Tips

- オブジェクトが必要な場合は `model` を使います。
- `default` は推奨ですが必須ではありません。

## 関連

- はじめに: [getting-started.ja.md](getting-started.ja.md)
- 設定（stylesheets）: [configuration.ja.md](configuration.ja.md)
