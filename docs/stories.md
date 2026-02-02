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

## Preview Wrapper

Use `preview.wrapper` to align the preview with your app's layout, theme, or fonts.
The wrapper is rendered inside the Shadow DOM and must include `{{content}}`.

```yaml
preview:
  wrapper: |
    <div data-theme="light" class="bg-base-200 px-6 py-6 text-base-content font-display">
      <div class="max-w-6xl mx-auto">
        {{content}}
      </div>
    </div>
```

## URL Behavior

- Story URL: `/thymeleaflet/{templatePath}/{fragmentName}/{storyName}`
- If `/default` is requested but no `default` story exists, Thymeleaflet redirects
  to the *first* story in the file.

## Tips

- Use `model` for fragments that require objects (not simple parameters).
- `default` is recommended but not required.
