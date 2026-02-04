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

## プレビューラッパー

`preview.wrapper` でアプリ本体のレイアウトやテーマを再現できます。
Shadow DOM 内で描画されるため、`{{content}}` を必ず含めてください。

```yaml
preview:
  wrapper: |
    <div data-theme="light" class="bg-base-200 px-6 py-6 text-base-content font-display">
      <div class="max-w-6xl mx-auto">
        {{content}}
      </div>
    </div>
```

## URL の挙動

- URL: `/thymeleaflet/{templatePath}/{fragmentName}/{storyName}`
- `/default` が指定されたが `default` が存在しない場合、ファイルの**先頭ストーリー**へリダイレクトします。

## Tips

- オブジェクトが必要な場合は `model` を使います。
- `default` は推奨ですが必須ではありません。

## 関連

- はじめに: [getting-started.ja.md](getting-started.ja.md)
- 設定（stylesheets）: [configuration.ja.md](configuration.ja.md)
