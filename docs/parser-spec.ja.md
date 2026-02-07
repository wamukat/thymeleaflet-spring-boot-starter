# Thymeleaf フラグメントパーサ仕様（ドラフト v1）

この文書は、Epic #39 で進める新パーサの対象仕様を定義します。  
対象は `th:fragment="..."` 宣言の解析と、発見/UI で使う正規化結果です。

## 1. 基準バージョン

- 文法基準: **Thymeleaf 3.1.2.RELEASE**
- 主参照: `org.thymeleaf.standard.expression.FragmentSignatureUtils`

本仕様は段階導入として次を分けて定義する:

- **解析互換集合**: Thymeleaf側パーサが受理する宣言
- **UIサポート集合(v1)**: Thymeleafletで表示/編集の安定保証を行う範囲

## 2. 目的

- 正規表現ベースの断片的な解析から移行する。
- 実用的な `th:fragment` 宣言を決定的に解析できるようにする。
- UI やストーリー表示で安定して使える出力を提供する。

## 3. 解析スコープ

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

## 4. サポート文法（v1 UIサポート集合）

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

### 4.1 正規化ルール

- 前後空白は無視する。
- パラメータ順は宣言順を保持する。
- `fragmentName()` は有効（パラメータなし）。
- 同名パラメータ重複は保持し、警告を出す（自動修正しない）。

## 5. 解析互換とUIサポートの関係

- 解析互換は Thymeleaf 3.1.2 の受理仕様を基準とする。
- Thymeleaflet v1 の UIサポートは、解析互換より意図的に狭くする。
- 解析はできるが UIサポート外の宣言は:
  - 発見結果には残す
  - `UNSUPPORTED_SYNTAX` を診断として付与
  - その宣言に対する厳密なUI編集機能は制限する

## 6. v1 で非対応（UIサポート集合）

- 識別子以外のフラグメント名形式（selector 形式など）
- 宣言内のデフォルト値/代入式
- 宣言内の複雑式や入れ子構文
- 識別子ではないパラメータトークン

非対応を検出した場合:

- `UNSUPPORTED_SYNTAX` 診断を出す
- その宣言は正規化結果を作らない
- 他宣言の解析は継続する

## 7. 曖昧ケースの扱い

- 括弧不整合は `INVALID_SIGNATURE`
- 空トークン（例: `a,,b`）は `INVALID_SIGNATURE`
- 識別子不正は `INVALID_SIGNATURE`
- 1テンプレート内に複数宣言がある場合は、出現順で返す

## 8. 入力例と期待結果

| `th:fragment` 値 | 結果 |
| --- | --- |
| `profileCard` | name=`profileCard`, params=`[]` |
| `profileCard()` | name=`profileCard`, params=`[]` |
| `profileCard(name, age)` | name=`profileCard`, params=`[name, age]` |
| ` profileCard ( name , age ) ` | name=`profileCard`, params=`[name, age]` |
| `profileCard(name,,age)` | `INVALID_SIGNATURE` |
| `profileCard(name` | `INVALID_SIGNATURE` |
| `profileCard(name='x')` | `th:fragment` の宣言側構文としては非対応のため `UNSUPPORTED_SYNTAX` |

## 9. 実装Issue向けテスト要件

- 受理/拒否ケースの単体テスト
- 正規化順序のゴールデンテスト
- sample テンプレート回帰テスト
- 非対応/不正入力の診断テスト
