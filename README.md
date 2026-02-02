# Thymeleaflet Spring Boot Starter

Thymeleaflet provides a lightweight Storybook-style UI for Thymeleaf fragments.
It discovers fragments, renders previews, and generates usage examples based on
JavaDoc-like comments embedded in HTML templates.

- Fragment list UI and preview pages under a configurable base path
- JavaDoc parsing for fragment parameters, models, and usage examples
- YAML-driven story configuration
- Built-in CSS (Tailwind) for the UI

## Positioning (Development Tool)

Thymeleaflet is a developer tool intended for local or internal use during
development. It should not be exposed in production environments.

Recommended setup (enable in dev only, disable in prod):

```yaml
# application-dev.yml
thymeleaflet:
  base-path: /thymeleaflet
  security:
    enabled: false

# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

You can also remove the dependency from production builds entirely.

## Requirements

- Java 17+
- Spring Boot 3.1+
- Maven or Gradle

## Installation

Maven:

```xml
<dependency>
  <groupId>io.github.wamukat</groupId>
  <artifactId>thymeleaflet-spring-boot-starter</artifactId>
  <version>0.0.1</version>
</dependency>
```

Gradle:

```kotlin
dependencies {
    implementation("io.github.wamukat:thymeleaflet-spring-boot-starter:0.0.1")
}
```

## Quick Start

1) Add the dependency and start your Spring Boot app.
2) Open the UI (default):

```
http://localhost:6006/thymeleaflet
```

3) Place fragment templates under your normal Thymeleaf templates path.
4) (Optional) Add story files under:

```
META-INF/thymeleaflet/stories/{templatePath}.stories.yml
```

## Configuration

```yaml
thymeleaflet:
  base-path: /thymeleaflet
  debug: false
  resources:
    template-paths:
      - /templates/
    stylesheets: []
    cache-duration-seconds: 3600
  security:
    enabled: true
  migration:
    phase: "4.0"
    monitoring:
      response-time-degradation-threshold: 10
      error-rate-increase-threshold: 1
      enforce-contract-tests: true
```

### Notes

- `thymeleaflet.base-path` controls the UI base path.
- `resources.template-paths` must contain 1 to 5 entries.
- `resources.stylesheets` supports up to 10 entries.
- `security.enabled` toggles the built-in security configuration for `/thymeleaflet/**`.

## Endpoints

- `{basePath}`: fragment list UI
- `{basePath}/main-content`: lazy-loaded main content
- `{basePath}/{templatePath}/{fragmentName}/{storyName}`: story preview page
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/content`: HTMX content fragment
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/render`: dynamic render endpoint
- `{basePath}/{templatePath}/{fragmentName}/{storyName}/usage`: usage example fragment

## JavaDoc in HTML Templates

Thymeleaflet parses JavaDoc-style comments embedded in HTML templates. Example:

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

## Build from Source

```bash
./mvnw test
npm install
npm run build
```

## Documentation

See:

- [docs/README.md](docs/README.md)
- [docs/getting-started.md](docs/getting-started.md)
- [docs/configuration.md](docs/configuration.md)
- [docs/javadoc.md](docs/javadoc.md)
- [docs/stories.md](docs/stories.md)
- [docs/security.md](docs/security.md)

## Contributing

See `CONTRIBUTING.md`.

## Security

See `SECURITY.md`.

## License

Apache-2.0. See `LICENSE`.

## 日本語

日本語版ドキュメントは `README.ja.md` を参照してください。
