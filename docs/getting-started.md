# Getting Started

## Requirements

- Java 17+
- Spring Boot 3.1+

## Install

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

1) Add the dependency.
2) Start your Spring Boot app.
3) Open the UI:

```
http://localhost:6006/thymeleaflet
```

## Development-only Usage

Thymeleaflet is intended for development and internal preview workflows.
Do not expose it in production. Use profiles to enable in dev and disable in prod:

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

Alternatively, remove the dependency from production builds.

## Project Layout

- Templates: `src/main/resources/templates/**`
- Stories: `src/main/resources/META-INF/thymeleaflet/stories/**`

## First Fragment and Story

1) Create a fragment in a template file.
2) Add a story file for the same template path.
3) Reload the UI and select your fragment.

See `stories.md` for the YAML format.

## Next Steps

- Configure base path and resources: `configuration.md`
- Add JavaDoc-style annotations: `javadoc.md`
- Define stories and preview wrappers: `stories.md`
