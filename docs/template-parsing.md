# Template Parsing Architecture

This document explains how Thymeleaflet parses templates for preview rendering, model inference, and JavaDoc examples. It covers the implemented runtime behavior, while [parser-spec.md](parser-spec.md) and [parser-model.md](parser-model.md) describe fragment declaration parsing goals and output shape.

## Layers

Template parsing is intentionally split into small layers:

1. `StructuredTemplateParser`
   - Uses Attoparser with Thymeleaf-compatible HTML configuration.
   - Extracts elements, attributes, text nodes, and comments with source positions.
   - Keeps template-level extraction away from regular expressions so quoted `>`, multiline attributes, comments, and `data-th-*` attributes are handled consistently.
2. `TemplateModelExpressionAnalyzer`
   - Reads attributes and text nodes from `StructuredTemplateParser`.
   - Collects `${...}` expressions from template content.
   - Infers model paths, loop aliases, local `th:with` aliases, referenced child templates, and no-argument method paths.
3. Thymeleaf expression tokenizer
   - Tokenizes identifiers, strings, dots, safe-navigation dots, parentheses, brackets, utility prefixes, and operators.
   - Extracts model paths from the expression subset needed by preview model inference.
   - Ignores utility object names such as `#temporals`, static class references such as `T(java.time.LocalDate)`, parameters, local aliases, and Thymeleaf reserved words.
   - Fails closed for unsupported bracket expressions such as `view.map[key]`; the stable prefix is kept, but dynamic keys are not inferred as model paths.
4. `JavaDocAnalyzer`
   - Parses JavaDoc-style HTML comments for `@param`, `@model`, `@fragment`, `@example`, and `@background`.
   - Uses `StructuredTemplateParser` for `@example` markup so `th:replace` and `data-th-replace` examples follow the same attribute parsing rules as templates.

## Supported Template Syntax

The model inference layer supports both `th:*` and `data-th-*` forms for the attributes below.

## Fragment Syntax Support Matrix

Status meanings:

- Supported: parsed and used by runtime or static analysis.
- Diagnostic-only: recognized as unsupported or dynamic without failing rendering.
- Unsupported: not currently parsed with stable semantics.
- Intentionally unsupported: accepted only by Thymeleaf rendering or deliberately left out of static analysis.

| Syntax | Status | Notes | Follow-up |
| --- | --- | --- | --- |
| `th:fragment="profileCard"` | Supported | Fragment declaration is discovered and displayed as a simple fragment. | Keep covered by declaration parser tests. |
| `th:fragment="profileCard()"` | Supported | Empty parameter list is normalized as a no-argument fragment. | Keep covered by declaration parser tests. |
| `th:fragment="profileCard(name, age)"` | Supported | Identifier parameters are preserved in declaration order. | Keep covered by declaration parser tests. |
| `data-th-fragment="profileCard(name)"` | Supported | Discovery treats `data-th-fragment` like `th:fragment`. | Keep covered by discovery tests. |
| Duplicate declaration parameters | Supported as-is | Duplicate names are currently preserved in declaration order; no uniqueness diagnostic is emitted yet. | Add a direct diagnostic if UI editing starts relying on uniqueness. |
| Declaration parameter defaults or assignment syntax | Unsupported | Declaration-side syntax such as `profileCard(name='x')` is outside the v1 UI support set. | Revisit only if real templates need it. |
| Non-identifier declaration names or parameters | Unsupported | Thymeleaf-compatible parsing may accept more than the UI support set; Thymeleaflet keeps normalized output narrow. | Keep as diagnostic rather than normalizing speculatively. |
| `~{components/card :: card(title=${view.title})}` | Supported | Static template path, selector, and argument list are parsed for dependency and model inference. | Keep covered by `FragmentExpressionParserTest`. |
| `~{'components/card' :: card(title='Ready')}` | Supported | Quoted template paths and literal arguments are supported. Literal-only calls skip child model recursion. | Keep covered by parser corpus. |
| `~{"components/card" :: content}` | Supported | Double-quoted template paths are supported. | Keep covered by parser tests. |
| `~{components/topbar :: topbar()}` | Supported | No-argument calls are normalized and do not recurse into child model requirements. | Keep covered by no-arg preprocessing tests. |
| Named call arguments, for example `card(title=${view.title})` | Supported as raw arguments | Argument names and values are preserved as raw segments; they are not mapped to declaration parameters semantically. | Consider semantic named-argument mapping only if preview value ordering needs it. |
| `th:replace`, `th:insert`, `th:include` | Supported | Static fragment expressions are analyzed. Unsupported or dynamic references are skipped non-fatally. | Centralize the shared attribute policy across analyzers. |
| `data-th-replace`, `data-th-insert`, `data-th-include` | Supported | `data-th-*` variants are included in parsing and diagnostics where relevant. | Centralize the shared attribute policy across analyzers. |
| `${dynamicRef}` as a fragment reference | Diagnostic-only | Dynamic references cannot be resolved statically and produce non-fatal diagnostics. | Keep diagnostic surfaced in story diagnostics. |
| Malformed fragment expressions | Diagnostic-only | Malformed static references produce non-fatal diagnostics. | Improve source location for expression diagnostics when possible. |
| Same-template references such as `~{:: header}` | Supported with current-template context | Static analyzers resolve the empty template path to the current template path. Parser calls without current-template context still fail closed. | Keep covered by parser, dependency, and model inference tests. |
| Same-template references such as `~{this :: header}` | Supported with current-template context | Static analyzers resolve `this` to the current template path. Parser calls without current-template context still fail closed. | Keep covered by parser, dependency, and model inference tests. |
| Selector-style references such as `~{template :: #header}` | Unsupported | CSS selector semantics are not normalized into a fragment name today. | Medium-value candidate; requires UI naming and matching rules. |
| Whole-template references such as `~{template}` | Intentionally unsupported for fragment inference | Thymeleaf can render template-level references, but Thymeleaflet fragment dependency inference needs a selector. | Keep skipped unless a concrete preview workflow needs it. |
| Template path expressions such as `~{${view.template} :: card}` | Diagnostic-only | Dynamic template paths are skipped because dependency targets are unknowable statically. | Keep non-fatal; do not infer speculative paths. |
| Fragment expression parameters with nested parentheses or quoted commas | Supported | Top-level splitting preserves nested expressions and quoted separators. | Keep covered by parser tests. |
| Fragment expression parameters with unbalanced parentheses or quotes | Diagnostic-only | Parse fails closed and emits diagnostics. | Keep covered by parser tests. |

Recommended support order:

1. Selector-style references such as `#id` or `.class`, only after matching and UI display rules are specified.
2. Semantic named-argument mapping, only if story value ordering needs declaration-aware argument binding.

Story diagnostic surfaces can carry multiple non-fatal parser diagnostics. YAML load diagnostics remain single-source diagnostics; parser diagnostics are summarized for users and retain developer details server-side.

### `th:each`

`th:each` binds loop aliases to the iterable model path. The first inferred path in the iterable expression becomes the source path for each alias.

```html
<article th:each="item : ${view.items}">
  <span th:text="${item.label}"></span>
</article>
```

This records `item -> view.items` and keeps `item.label` available for downstream inference.

Tuple-style aliases are accepted when each alias is an identifier:

```html
<div th:each="(label, value) : ${view.options}"></div>
```

### `th:with`

`th:with` local variable names are excluded from required model paths.

```html
<section th:with="current=${view.currentUser}">
  <span th:if="${current.active()}"></span>
</section>
```

`view.currentUser` is inferred from the assignment expression, while `current` is treated as a local alias.

### `th:replace` and `th:insert`

Literal fragment expressions are used to infer referenced child template paths.

```html
<th:block th:replace="~{components/card :: card(title=${view.title})}"></th:block>
<th:block th:insert="~{'components/list' :: list(items=${view.items})}"></th:block>
```

Expression-based references such as `${body}` are ignored for dependency inference because the target cannot be known statically.

Child model recursion is skipped when a fragment call has an empty argument list or all arguments are literals:

```html
<div th:replace="~{components/topbar :: topbar()}"></div>
<div th:replace="~{components/badge :: badge(label='Ready')}"></div>
```

If any argument depends on a model expression, the child template remains a recursive inference target.

## Supported Expression Subset

Model path inference is intentionally conservative. Supported examples include:

```html
<span th:text="${view.profile.name}"></span>
<time th:text="${#temporals.format(item.publishedAt, 'yyyy-MM-dd')}"></time>
<span th:if="${T(java.time.LocalDate).now().isAfter(view.cutoffDate)}"></span>
<span th:text="${view['display-name']}"></span>
```

The inferred paths are `view.profile.name`, `item.publishedAt`, `view.cutoffDate`, and `view.display-name`.

The analyzer does not try to fully evaluate Thymeleaf or Spring EL. Unsupported constructs are ignored or reduced to a stable prefix instead of producing speculative paths.

## JavaDoc `@model`

`@model` documents model values that stories and previews may need. It also drives type-aware story value coercion, including nested list paths.

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

Use `[]` in `@model` paths to document values inside lists. For story data nested under the default `view` root, relative list paths can also be matched during coercion when no literal path exists.

## Regression Test Guidance

Add parser regressions when a defect involves template attributes, JavaDoc examples, model inference, or child fragment references.

- Add a focused unit test near the parser or analyzer that owns the behavior.
- Add or extend `src/test/resources/templates/regression/parser-corpus.html` when the behavior should be preserved as an HTML fixture.
- Add a story under `src/test/resources/META-INF/thymeleaflet/stories/regression/` when the behavior affects preview rendering.
- For rendering regressions, add an integration test in `ThymeleafletRenderingExceptionHandlerIntegrationTest`.
- Run focused tests first, then full Maven tests, then E2E:

```bash
./mvnw -q -Dtest=TemplateModelExpressionAnalyzerTest,StructuredTemplateParserTest,JavaDocAnalyzerTest test
./mvnw test -q
./mvnw -DskipTests install -q && npm run test:e2e:local
```

When adding syntax support, prefer parser-owned tests over broad end-to-end assertions. E2E should prove the user-facing preview still renders; unit and integration tests should define the parsing contract.

## External Parser Evaluation

`HtmlParserAdapterComparisonTest` is the comparison spike for evaluating external HTML parsers against the Thymeleaflet parser corpus. The current candidate is jsoup, added only as a test-scoped dependency. The reusable test-support contract in `io.github.wamukat.thymeleaflet.testsupport.parser` runs the regression corpus through both `StructuredTemplateParser` and a candidate adapter, then compares these contract points:

- Thymeleaf attribute names and values are preserved, including `th:*`, `data-th-*`, quoted fragment selectors, multiline values, literal `>` characters, and boolean-adjacent attributes.
- Fragment declarations remain discoverable in source order.
- Browser-tolerated malformed HTML remains parseable enough for fragment and model extraction.
- Subtree extraction can keep sibling fragments isolated.

Recommendation: keep `StructuredTemplateParser` on Attoparser for production parsing. Attoparser uses Thymeleaf-compatible HTML parsing and exposes source line/column positions used by diagnostics. jsoup is useful as a corpus comparison tool and fallback research candidate, but it normalizes the document tree and does not provide the source positions required by current diagnostics. Do not replace the production parser until a future adapter proves equivalent on the corpus and provides a plan for source locations and Thymeleaf-specific parsing behavior.
