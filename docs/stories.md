# Stories (YAML)

Story files are loaded from:

```
META-INF/thymeleaflet/stories/{templatePath}.stories.yml
```

## Example

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

## Schema

- `meta`
  - `title`: overall title
  - `description`: overall description
- `storyGroups`
  - Key: fragment name
  - `title`, `description`
  - `stories`: list of story items

### Story Item

- `name`: story name (used in URL)
- `title`: display title
- `description`: optional description
- `parameters`: fragment parameters
- `model`: model values to inject
- `methodReturns`: no-arg method return values for preview
- `preview.wrapper`: optional HTML wrapper for preview

## Model

Use `model` when a fragment expects objects rather than simple parameters.
The `model` map is merged into the Thymeleaf model before rendering.

### Example

Fragment example (expects `profile`):

```html
<div th:fragment="profileCard()">
  <h2 th:text="${profile.name}">Name</h2>
  <p th:text="${profile.role}">Role</p>
</div>
```

Story example:

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

### Notes

- `model` supports nested maps and lists (YAML objects/arrays).
- `model` and `parameters` can be combined in the same story.
- If a model key is missing, Thymeleaf expressions may evaluate to `null`.

## Java Time Values

Story YAML values are parsed as YAML scalars first. When JavaDoc declares a
`java.time` type for a parameter or model path, Thymeleaflet converts ISO string
values before rendering.

Supported targets:

- `LocalDate`
- `LocalDateTime`
- `LocalTime`
- `OffsetDateTime`
- `ZonedDateTime`
- `Instant`

Parameter example:

```html
/**
 * @param publishedAt {@code java.time.LocalDateTime} [required] Published date
 */
```

```yaml
parameters:
  publishedAt: "2026-04-01T10:00:00"
```

Nested model list example:

```html
/**
 * @model view.items[].publishedAt {@code java.time.LocalDateTime} [required] Published date
 */
```

```yaml
model:
  view:
    items:
      - title: Notice 1
        publishedAt: "2024-06-01T10:00:00"
```

## methodReturns (No-arg)

Use `methodReturns` to control no-arg method calls in preview rendering.

```yaml
stories:
  - name: default
    title: Default
    methodReturns:
      view:
        pointPage:
          hasPrev: false
          nextPage: 2
```

- `methodReturns` uses the same nested-map shape as `model`.
- Target scope is preview `/render` only.
- If `model` and `methodReturns` define the same path, Thymeleaflet records a warning and keeps the `model` value.

### No-arg Method Compatibility (Preview `/render` only)

Preview rendering supports a compatibility layer for **no-arg** method calls on map-like story models.

- Supported pattern examples:
  - `view.pointPage.hasPrev()`
  - `view.pointPage.isActive()`
  - `view.pointPage.nextPage()`
- Resolution rule:
  - Tries map keys in order: method name itself, then derived key (`hasPrev()` -> `hasPrev`, `prev`-style suffix key: `prev` forms become `prev...` lower-camel).
  - If unresolved, preview continues with `null` and records a warning.
- Scope:
  - Applied only to Thymeleaflet preview `/render`.
  - Does **not** change normal Spring MVC page rendering behavior.
- Out of scope:
  - Methods with arguments (for example `format('yyyy')`)
  - Arbitrary business logic execution
  - External dependency calls

## Preview Wrapper

Use `preview.wrapper` to align the preview with your app's layout, theme, or fonts.
The wrapper is rendered inside the preview iframe and must include `{{content}}`.
The sample stories use wrappers to match the daisyUI layout and spacing.

```yaml
preview:
  wrapper: |
    <div data-theme="light" class="bg-base-200 px-6 py-6 text-base-content font-display">
      <div class="max-w-6xl mx-auto">
        {{content}}
      </div>
    </div>
```

## Custom Story (UI Overrides)

Thymeleaflet adds a **Custom** story entry in the UI so you can edit parameters directly.

- **Initial values**: copied from the `default` story if it exists, otherwise from the first story.
- **Persistence**: stored in `sessionStorage` per fragment.
- **Editable fields**: `parameters`, `model`, and `methodReturns`.
- **Scope**: affects preview rendering only (does not write back to `stories.yml`).
- **Reserved name**: `custom` is reserved by the UI and should not be defined in stories.yml.

Type inference and fallback (when no story values exist):

- If type information is available, Thymeleaflet prioritizes that type: `List`/arrays are initialized as arrays, and `Map` as objects.
- If type information is unavailable, initial values are inferred from parameter names (for example, `options`/`items`/`rows` default to arrays).
- For production use, define explicit values in `stories.yml` for parameters with strict shape requirements, especially array-style inputs such as `options`.

## URL Behavior

- Story URL: `/thymeleaflet/{templatePath}/{fragmentName}/{storyName}`
- If `/default` is requested but no `default` story exists, Thymeleaflet redirects
  to the *first* story in the file.

## Safe Fragment Parameters

When a parameter is used with `th:replace` or `th:insert`, pass a fragment expression (`~{...}`).

Unsafe example:

```html
<th:block th:replace="${body}"></th:block>
```

Why this is unsafe:

- A plain string like `body: value` can be treated as a template name and trigger `TemplateInputException`.

Recommended safe branching:

```html
<th:block th:if="${body != null and #strings.startsWith(body.toString(), '~')}">
  <th:block th:replace="~{${body}}"></th:block>
</th:block>
<p th:if="${body != null and !#strings.startsWith(body.toString(), '~')}" th:text="${body}"></p>
```

Story parameter design guide:

- For `th:replace`/`th:insert`, require values in `~{template :: fragment(...)}` format.
- Do not pass generic dummy text values directly into `th:replace`.
- Use separate names for text vs fragment reference values (for example, `bodyText` and `bodyFragment`).

## Tips

- Use `model` for fragments that require objects (not simple parameters).
- `default` is recommended but not required.

## Related

- Getting started: [getting-started.md](getting-started.md)
- Configuration (stylesheets): [configuration.md](configuration.md)
