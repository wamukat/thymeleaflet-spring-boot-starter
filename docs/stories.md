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
- **Editable fields**: both `parameters` and `model`.
- **Scope**: affects preview rendering only (does not write back to `stories.yml`).
- **Reserved name**: `custom` is reserved by the UI and should not be defined in stories.yml.

## URL Behavior

- Story URL: `/thymeleaflet/{templatePath}/{fragmentName}/{storyName}`
- If `/default` is requested but no `default` story exists, Thymeleaflet redirects
  to the *first* story in the file.

## Tips

- Use `model` for fragments that require objects (not simple parameters).
- `default` is recommended but not required.

## Related

- Getting started: [getting-started.md](getting-started.md)
- Configuration (stylesheets): [configuration.md](configuration.md)
