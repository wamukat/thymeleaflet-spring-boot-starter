# JavaDoc in HTML Templates

Thymeleaflet parses JavaDoc-style comments embedded in HTML templates to extract
fragment documentation, parameters, and usage examples. This is optional but
recommended for richer previews.

## Basic Format

```html
<!--
/**
 * Member detail (memberDetail)
 *
 * @param variant {@code String} [optional=standard] Display variant
 * @model memberProfile {@code MemberProfile} [required] Member model
 * @example <div th:replace="~{domain/member/organisms/member-profile :: memberDetail()}"></div>
 * @background gray-50
 */
-->
```

## Tags

- `@param name {@code Type} [required|optional=default] Description`
- `@model name {@code Type} [required|optional=default] Description`
- `@example <div th:replace="~{path :: fragment(...)}"></div>`
- `@background` for preview background styling

## Notes

- `@example` is used to link JavaDoc metadata with a fragment.
- Parameters should include type and requirement to be rendered correctly.
- Place the JavaDoc block inside an HTML comment (`<!-- ... -->`).

## Related

- Getting started: [getting-started.md](getting-started.md)
- Stories: [stories.md](stories.md)
