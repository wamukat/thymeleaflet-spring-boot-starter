# Thymeleaf フラグメントパーサ仕様（ドラフト v1）

この文書は、Epic #39 で進める新パーサの対象仕様を定義します。  
対象は `th:fragment="..."` 宣言の解析と、発見/UI で使う正規化結果です。

## 1. 目的

- 正規表現ベースの断片的な解析から移行する。
- 実用的な `th:fragment` 宣言を決定的に解析できるようにする。
- UI やストーリー表示で安定して使える出力を提供する。

## 2. 解析スコープ

### 2.1 入力

- 対象ファイル: 設定された template path 配下の HTML
- 対象属性: `th:fragment`
- クォート: シングル/ダブルを両方サポート
- 配置場所: 任意の要素

### 2.2 出力

1宣言あたり次を出力する:

- `fragmentName`: 正規化されたフラグメント名
- `parameters`: 順序付きパラメータ一覧
- `originalDefinition`: 元の `th:fragment` 文字列
- `diagnostics`: 警告/エラー（位置情報付き）

## 3. サポート文法（v1）

```ebnf
signature      = fragmentName , [ ws , "(" , ws , [ parameterList ] , ws , ")" ] ;
parameterList  = parameter , { ws , "," , ws , parameter } ;
parameter      = identifier ;
fragmentName   = identifier ;
identifier     = firstChar , { nextChar } ;
firstChar      = "A".."Z" | "a".."z" | "0".."9" ;
nextChar       = firstChar | "_" | "-" ;
ws             = { " " | "\t" | "\n" | "\r" } ;
```

### 3.1 正規化ルール

- 前後空白は無視する。
- パラメータ順は宣言順を保持する。
- `fragmentName()` は有効（パラメータなし）。
- 同名パラメータ重複は保持し、警告を出す（自動修正しない）。

## 4. v1 で非対応

- 識別子以外のフラグメント名形式（selector 形式など）
- 宣言内のデフォルト値/代入式
- 宣言内の複雑式や入れ子構文
- 識別子ではないパラメータトークン

非対応を検出した場合:

- `UNSUPPORTED_SYNTAX` 診断を出す
- その宣言は正規化結果を作らない
- 他宣言の解析は継続する

## 5. 曖昧ケースの扱い

- 括弧不整合は `INVALID_SIGNATURE`
- 空トークン（例: `a,,b`）は `INVALID_SIGNATURE`
- 識別子不正は `INVALID_SIGNATURE`
- 1テンプレート内に複数宣言がある場合は、出現順で返す

## 6. 入力例と期待結果

| `th:fragment` 値 | 結果 |
| --- | --- |
| `profileCard` | name=`profileCard`, params=`[]` |
| `profileCard()` | name=`profileCard`, params=`[]` |
| `profileCard(name, age)` | name=`profileCard`, params=`[name, age]` |
| ` profileCard ( name , age ) ` | name=`profileCard`, params=`[name, age]` |
| `profileCard(name,,age)` | `INVALID_SIGNATURE` |
| `profileCard(name` | `INVALID_SIGNATURE` |
| `profileCard(name='x')` | `UNSUPPORTED_SYNTAX` |

## 7. 互換性ポリシー

- 単純シグネチャは既存互換とする。
- これまで曖昧に通っていた文字列は、明示的エラーになる場合がある。
- 一部失敗しても、一覧全体が壊れないよう UI へ診断を渡す。

## 8. 実装Issue向けテスト要件

- 受理/拒否ケースの単体テスト
- 正規化順序のゴールデンテスト
- sample テンプレート回帰テスト
- 非対応/不正入力の診断テスト
