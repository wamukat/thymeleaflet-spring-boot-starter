# フラグメントパーサ出力モデル（ドラフト v1）

この文書は Issue #49 で設計する、パーサ出力モデル案を定義します。

## 1. 設計目標

- パーサ出力を不変・UI利用しやすい形にする。
- 宣言順とソース位置を保持する。
- `未指定` と `空文字` を明確に区別できるようにする。

## 2. 提案コア型

## `FragmentDeclaration`

- `templatePath: String`
- `fragmentName: String`
- `parameters: List<FragmentParameter>`
- `originalDefinition: String`
- `location: SourceLocation`
- `diagnostics: List<FragmentDiagnostic>`

## `FragmentParameter`

- `name: String`
- `order: int`（宣言順）
- `status: ParameterStatus`  
  パーサ段階では基本 `DECLARED`。UI 連携時に `MISSING`/`EMPTY_STRING`/`SET` を付与
- `source: ParameterSource`（`SIGNATURE`/`JAVADOC`/`STORY`）

## `FragmentDiagnostic`

- `code: DiagnosticCode`
- `severity: DiagnosticSeverity`
- `message: String`
- `location: SourceLocation`

## `SourceLocation`

- `line: int`
- `column: int`

## 3. 列挙型

## `DiagnosticCode`

- `INVALID_SIGNATURE`
- `UNSUPPORTED_SYNTAX`
- `DUPLICATE_PARAMETER`
- `UNKNOWN`

## `DiagnosticSeverity`

- `ERROR`
- `WARNING`
- `INFO`

## `ParameterStatus`（主にUI）

- `DECLARED`
- `MISSING`
- `EMPTY_STRING`
- `SET`

## `ParameterSource`

- `SIGNATURE`
- `JAVADOC`
- `STORY`

## 4. マージ方針（Parser + JavaDoc + Story）

1. 基本の順序はシグネチャ宣言順（`SIGNATURE`）を採用
2. JavaDoc情報はパラメータ名でマージ
3. Story値ステータスは実行時に重ねる
   - キーなし -> `MISSING`
   - キーありかつ空文字 -> `EMPTY_STRING`
   - それ以外 -> `SET`
4. シグネチャにないがJavaDoc/storyにある項目は末尾追加＋警告

## 5. 互換移行方針

- 現行 `FragmentInfo.parameters: List<String>` は新モデルから変換可能
- まず既存UIレスポンス互換を維持し、#53 で新形へ移行
- #50-#53 は旧Regex経路と併走可、#54 で撤去
