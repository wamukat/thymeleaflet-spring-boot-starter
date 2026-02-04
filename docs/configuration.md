# Configuration

All properties are under the `thymeleaflet` prefix.
This page covers *only* configuration. Usage details are in
[getting-started.md](getting-started.md) and [stories.md](stories.md).

## Base Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.base-path` | String | `/thymeleaflet` | Base URL for the UI |
| `thymeleaflet.debug` | boolean | `false` | Enables debug logging for fragment discovery |

## Resource Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.resources.template-paths` | List<String> | [`/templates/`] | Paths to scan for templates (1-5) |
| `thymeleaflet.resources.stylesheets` | List<String> | `[]` | CSS paths injected into preview (max 10) |
| `thymeleaflet.resources.cache-duration-seconds` | int | `3600` | Cache duration for resources |

`resources.stylesheets` are injected into the **Shadow DOM preview** only.
Use `preview.wrapper` in [stories.md](stories.md) to align layout and theme.
The sample app injects `/css/mypage.css` and `/css/mypage/components.css`
so the preview matches the app styling.

## Security

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.security.enabled` | boolean | `true` | Enables Thymeleaflet security filter chain |

## Enable/Disable by Environment

Thymeleaflet is intended for development use. In production, disable it by
excluding the auto-configuration (or remove the dependency from prod builds).

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## Internal (Migration)

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.migration.phase` | String | `"4.0"` | Feature toggle / migration phase |
| `thymeleaflet.migration.monitoring.response-time-degradation-threshold` | int | `10` | Internal monitoring setting |
| `thymeleaflet.migration.monitoring.error-rate-increase-threshold` | int | `1` | Internal monitoring setting |
| `thymeleaflet.migration.monitoring.enforce-contract-tests` | boolean | `true` | Internal monitoring setting |

## Example

```yaml
thymeleaflet:
  base-path: /thymeleaflet
  debug: false
  resources:
    template-paths:
      - /templates/
    stylesheets:
      - /css/app.css
    cache-duration-seconds: 3600
  security:
    enabled: true
```
