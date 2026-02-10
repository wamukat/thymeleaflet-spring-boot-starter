# Configuration

All properties are under the `thymeleaflet` prefix.
This page covers *only* configuration. Usage details are in
[getting-started.md](getting-started.md) and [stories.md](stories.md).

## Base Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.base-path` | String | `/thymeleaflet` | Base URL for the UI |
| `thymeleaflet.debug` | boolean | `false` | Enables debug logging for fragment discovery |

`thymeleaflet.base-path` currently supports only `/thymeleaflet`.
Using another path fails fast at startup.

## Resource Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.resources.template-paths` | List<String> | [`/templates/`] | Paths to scan for templates (1-5) |
| `thymeleaflet.resources.stylesheets` | List<String> | `[]` | CSS paths injected into preview (max 10) |
| `thymeleaflet.resources.scripts` | List<String> | `[]` | JS paths injected into preview iframe (max 10) |
| `thymeleaflet.resources.cache-duration-seconds` | int | `3600` | Cache duration for resources |

`resources.stylesheets` and `resources.scripts` are injected into the **iframe preview** only.
Use `preview.wrapper` in [stories.md](stories.md) to align layout and theme.
The sample app injects `/css/mypage.css` and `/css/mypage/components.css`
so the preview matches the app styling.
To use JavaScript in previews, register it in `resources.scripts`.
Example: `<div data-theme="light">{{content}}</div>`
Preview iframes allow same-origin so cookies/localStorage and authenticated API calls work.

## Preview Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.preview.background-light` | String | `#f3f4f6` | Light background color for the preview canvas |
| `thymeleaflet.preview.background-dark` | String | `#1f2937` | Dark background color for the preview canvas |
| `thymeleaflet.preview.viewports` | List | Built-in presets | Viewport presets (name + width, excluding Fit) |

Viewport presets are used by the viewport dropdown. Fit is always available and is not part of this list.
Each item supports `id`, `label`, and `width`.

## Cache Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.cache.enabled` | boolean | `true` | Enables in-memory caches for fragment discovery, JavaDoc parsing, and dependency analysis |
| `thymeleaflet.cache.preload` | boolean | `false` | Preload caches at startup (useful for low-CPU demo environments) |

## Security Helper Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `thymeleaflet.security.auto-permit` | boolean | `false` | Opt-in: register minimal permit rule for `/thymeleaflet/**` when Spring Security is present |

### CSP note (permissive by design)

Thymeleaflet sets a permissive CSP to allow external JS and CSS inside preview iframes.
This is convenient for development but **reduces protection**. Keep Thymeleaflet
behind authentication and use it only in trusted environments.

## Spring Security Integration

By default, Thymeleaflet does not add its own `SecurityFilterChain`.
Use one of the following options:

- Opt-in auto permit:

```yaml
thymeleaflet:
  security:
    auto-permit: true
```

- Explicit app-side security rule:

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/thymeleaflet/**").permitAll()
    .anyRequest().authenticated()
);
```

## Enable/Disable by Environment

Thymeleaflet is intended for development use. In production, disable it by
excluding the auto-configuration (or remove the dependency from prod builds).

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

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
    scripts:
      - /js/app.js
    cache-duration-seconds: 3600
  preview:
    background-light: "#f7f7f9"
    background-dark: "#1f2937"
    viewports:
      - id: mobileSmall
        label: Mobile Small
        width: 320
      - id: tablet
        label: Tablet
        width: 834
  cache:
    enabled: true
    preload: false
  security:
    auto-permit: false
```
