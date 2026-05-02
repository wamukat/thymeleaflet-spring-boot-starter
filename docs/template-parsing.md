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
