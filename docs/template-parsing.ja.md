# テンプレート解析アーキテクチャ

この文書は、Thymeleaflet がプレビュー描画、モデル推論、JavaDoc example のためにテンプレートをどう解析するかを説明します。ここでは実装済みの実行時挙動を扱い、[parser-spec.ja.md](parser-spec.ja.md) と [parser-model.ja.md](parser-model.ja.md) はフラグメント宣言解析の目標仕様と出力モデルを扱います。

## レイヤ

テンプレート解析は、小さなレイヤに分けて実装しています。

1. `StructuredTemplateParser`
   - Attoparser を Thymeleaf 互換の HTML 設定で使います。
   - 要素、属性、テキストノード、コメントを位置情報付きで抽出します。
   - quote 付きの `>`、複数行属性、コメント、`data-th-*` 属性を安定して扱うため、テンプレートレベルの抽出を正規表現に寄せません。
2. `TemplateModelExpressionAnalyzer`
   - `StructuredTemplateParser` から得た属性とテキストノードを読みます。
   - テンプレート内容から `${...}` 式を集めます。
   - モデルパス、loop alias、`th:with` local alias、参照先子テンプレート、引数なしメソッドパスを推論します。
3. Thymeleaf expression tokenizer
   - 識別子、文字列、dot、safe-navigation dot、括弧、bracket、utility prefix、operator を token 化します。
   - プレビューのモデル推論に必要な式のサブセットからモデルパスを抽出します。
   - `#temporals` などの utility 名、`T(java.time.LocalDate)` などの static class 参照、パラメータ、local alias、Thymeleaf の予約語はモデルパスにしません。
   - `view.map[key]` のような未対応 bracket 式は fail closed にします。安定した prefix は残しますが、dynamic key はモデルパスとして推論しません。
4. `JavaDocAnalyzer`
   - HTML コメント内の JavaDoc 形式から `@param`、`@model`、`@fragment`、`@example`、`@background` を解析します。
   - `@example` markup には `StructuredTemplateParser` を使い、`th:replace` と `data-th-replace` の例をテンプレート本体と同じ属性解析ルールで扱います。

## サポートするテンプレート構文

モデル推論レイヤは、以下の属性で `th:*` と `data-th-*` の両方をサポートします。

## Fragment Syntax Support Matrix

状態の意味:

- Supported: runtime または static analysis で解析・利用できる。
- Diagnostic-only: unsupported / dynamic として認識し、rendering は失敗させない。
- Unsupported: 安定した意味でまだ解析していない。
- Intentionally unsupported: Thymeleaf rendering には任せる、または static analysis 対象から意図的に外している。

| Syntax | Status | Notes | Follow-up |
| --- | --- | --- | --- |
| `th:fragment="profileCard"` | Supported | simple fragment として discovery / 表示できる。 | declaration parser test で維持する。 |
| `th:fragment="profileCard()"` | Supported | 空の parameter list を no-argument fragment として正規化する。 | declaration parser test で維持する。 |
| `th:fragment="profileCard(name, age)"` | Supported | identifier parameter を宣言順で保持する。 | declaration parser test で維持する。 |
| `data-th-fragment="profileCard(name)"` | Supported | discovery では `data-th-fragment` を `th:fragment` と同様に扱う。 | discovery test で維持する。 |
| duplicate declaration parameters | Supported as-is | duplicate name は現在 declaration order のまま保持し、uniqueness diagnostic はまだ出していない。 | UI editing が uniqueness に依存する前に直接 diagnostic を追加する。 |
| declaration parameter defaults / assignment syntax | Unsupported | `profileCard(name='x')` のような declaration 側 syntax は v1 UI support set の外。 | 実テンプレートで必要になった場合のみ再検討する。 |
| non-identifier declaration names / parameters | Unsupported | Thymeleaf が受け付ける範囲より、Thymeleaflet の正規化出力は狭く保つ。 | 推測で正規化せず diagnostic として扱う。 |
| `~{components/card :: card(title=${view.title})}` | Supported | static template path、selector、argument list を dependency / model inference に利用する。 | `FragmentExpressionParserTest` で維持する。 |
| `~{'components/card' :: card(title='Ready')}` | Supported | quote 付き template path と literal argument をサポートする。literal-only call は child model recursion を skip する。 | parser corpus で維持する。 |
| `~{"components/card" :: content}` | Supported | double quote 付き template path をサポートする。 | parser test で維持する。 |
| `~{components/topbar :: topbar()}` | Supported | no-argument call を正規化し、child model requirements へ再帰しない。 | no-arg preprocessing test で維持する。 |
| named call arguments, e.g. `card(title=${view.title})` | Supported as raw arguments | argument name/value は raw segment として保持する。declaration parameter への意味的 mapping は未実施。 | story value ordering が必要とする場合のみ semantic mapping を検討する。 |
| `th:replace`, `th:insert`, `th:include` | Supported | static fragment expression を解析する。unsupported / dynamic reference は non-fatal に skip する。 | analyzer 間で shared attribute policy を集約する。 |
| `data-th-replace`, `data-th-insert`, `data-th-include` | Supported | 関連箇所では `data-th-*` variant も parsing / diagnostics 対象に含める。 | analyzer 間で shared attribute policy を集約する。 |
| fragment reference としての `${dynamicRef}` | Diagnostic-only | dynamic reference は static に解決できないため non-fatal diagnostic を出す。 | story diagnostics への surfaced 状態を維持する。 |
| malformed fragment expressions | Diagnostic-only | malformed static reference は non-fatal diagnostic を出す。 | 可能なら expression diagnostic の source location を改善する。 |
| `~{:: header}` のような same-template reference | Supported with current-template context | static analyzer は空の template path を現在の template path として解決する。current-template context なしの parser call は fail closed のまま。 | parser / dependency / model inference test で維持する。 |
| `~{this :: header}` のような same-template reference | Supported with current-template context | static analyzer は `this` を現在の template path として解決する。current-template context なしの parser call は fail closed のまま。 | parser / dependency / model inference test で維持する。 |
| `~{template :: #header}` のような selector-style reference | Unsupported | CSS selector semantics は fragment name として正規化していない。 | matching / UI 表示ルールを定義してから検討する。 |
| `~{template}` のような whole-template reference | Intentionally unsupported for fragment inference | Thymeleaf は template-level reference を rendering できるが、Thymeleaflet の fragment dependency inference には selector が必要。 | 具体的な preview workflow が出るまでは skip する。 |
| `~{${view.template} :: card}` のような template path expression | Diagnostic-only | dynamic template path は dependency target が static に分からないため skip する。 | non-fatal のまま維持し、推測 path は作らない。 |
| nested parentheses / quoted commas を含む fragment expression parameter | Supported | top-level split により nested expression と quote 内 separator を保持する。 | parser test で維持する。 |
| unbalanced parentheses / quotes を含む fragment expression parameter | Diagnostic-only | fail closed し diagnostic を出す。 | parser test で維持する。 |

推奨サポート順:

1. story diagnostic surface での複数 parser diagnostics 表示。
2. `#id` や `.class` の selector-style references。matching と UI 表示ルールを先に決める。
3. Semantic named-argument mapping。story value ordering が declaration-aware binding を必要とする場合のみ進める。

### `th:each`

`th:each` は loop alias を iterable のモデルパスへ紐づけます。iterable 式の最初の推論パスが各 alias の source path になります。

```html
<article th:each="item : ${view.items}">
  <span th:text="${item.label}"></span>
</article>
```

この例では `item -> view.items` を記録し、`item.label` を後続の推論対象として保持します。

各 alias が識別子であれば、tuple 形式も受け付けます。

```html
<div th:each="(label, value) : ${view.options}"></div>
```

### `th:with`

`th:with` の local variable 名は required model path から除外されます。

```html
<section th:with="current=${view.currentUser}">
  <span th:if="${current.active()}"></span>
</section>
```

代入式から `view.currentUser` は推論されますが、`current` は local alias として扱われます。

### `th:replace` / `th:insert`

literal fragment expression は、参照先子テンプレートパスの推論に使われます。

```html
<th:block th:replace="~{components/card :: card(title=${view.title})}"></th:block>
<th:block th:insert="~{'components/list' :: list(items=${view.items})}"></th:block>
```

`${body}` のような expression-based reference は、静的に参照先を確定できないため dependency inference では無視します。

fragment call が空引数、または全引数が literal の場合は、子モデルの再帰推論をスキップします。

```html
<div th:replace="~{components/topbar :: topbar()}"></div>
<div th:replace="~{components/badge :: badge(label='Ready')}"></div>
```

引数のどれかが model expression に依存する場合、その子テンプレートは再帰推論対象のままにします。

## サポートする式のサブセット

モデルパス推論は意図的に保守的です。次のような例をサポートします。

```html
<span th:text="${view.profile.name}"></span>
<time th:text="${#temporals.format(item.publishedAt, 'yyyy-MM-dd')}"></time>
<span th:if="${T(java.time.LocalDate).now().isAfter(view.cutoffDate)}"></span>
<span th:text="${view['display-name']}"></span>
```

推論されるパスは `view.profile.name`、`item.publishedAt`、`view.cutoffDate`、`view.display-name` です。

Analyzer は Thymeleaf や Spring EL を完全評価しません。未対応構文は、推測で壊れたパスを作らず、無視するか安定した prefix に縮退します。

## JavaDoc `@model`

`@model` は story や preview が必要とする model value を記述します。ネストした list path を含む型考慮の story value coercion にも使われます。

```html
<!--
/**
 * Member card.
 *
 * @model view.member.name {@code String} [required] Member name
 * @model view.items[].publishedAt {@code java.time.LocalDateTime} [required] Published date
 * @example <div th:replace="~{components/member-card :: card()}"></div>
 */
-->
<article th:fragment="card">
  <h2 th:text="${view.member.name}"></h2>
</article>
```

list 内の値を記述する場合は `@model` path に `[]` を使います。story data が default の `view` root 配下にある場合、literal path が存在しなければ relative list path も coercion 時に照合されます。

## 回帰テスト追加ガイド

テンプレート属性、JavaDoc example、モデル推論、子 fragment 参照に関わる不具合では parser regression を追加してください。

- 挙動の責務を持つ parser/analyzer の近くに focused unit test を追加します。
- HTML fixture として保持すべき挙動は `src/test/resources/templates/regression/parser-corpus.html` を追加または更新します。
- preview rendering に影響する場合は `src/test/resources/META-INF/thymeleaflet/stories/regression/` に story を追加します。
- rendering regression は `ThymeleafletRenderingExceptionHandlerIntegrationTest` に integration test を追加します。
- focused test、full Maven test、E2E の順で実行します。

```bash
./mvnw -q -Dtest=TemplateModelExpressionAnalyzerTest,StructuredTemplateParserTest,JavaDocAnalyzerTest test
./mvnw test -q
./mvnw -DskipTests install -q && npm run test:e2e:local
```

構文サポートを追加する場合は、広い E2E assertion より parser-owned test を優先してください。E2E はユーザー向け preview が描画できることを確認し、unit/integration test で解析契約を定義します。

## 外部パーサ評価

`HtmlParserAdapterComparisonTest` は、外部 HTML パーサを Thymeleaflet の parser corpus に対して評価するための比較 spike です。現在の候補は jsoup で、依存は test scope のみに限定しています。`io.github.wamukat.thymeleaflet.testsupport.parser` の reusable test-support contract は regression corpus を `StructuredTemplateParser` と候補アダプタの両方で解析し、次の契約を比較します。

- `th:*`、`data-th-*`、quote 付き fragment selector、複数行値、literal `>`、boolean 属性に隣接する属性を含め、Thymeleaf 属性名と値を保持できること。
- fragment 宣言を source order で発見できること。
- browser tolerant な malformed HTML でも、fragment と model 抽出に必要な範囲を解析できること。
- subtree 抽出で sibling fragment を分離できること。

推奨方針: production parser は Attoparser ベースの `StructuredTemplateParser` を維持します。Attoparser は Thymeleaf 互換の HTML 解析を使え、diagnostics で利用する source line/column も提供できます。jsoup は corpus 比較ツールや fallback 候補の調査には有用ですが、document tree を正規化し、現行 diagnostics が必要とする source position を提供しません。将来の adapter が corpus 上で同等性を示し、source location と Thymeleaf 固有挙動の扱いを解決するまでは production parser を置き換えません。
