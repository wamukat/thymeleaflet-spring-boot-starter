# Security

When Spring Security is present, Thymeleaflet registers a minimal permit filter
chain for `/thymeleaflet/**` by default.
This tool is intended for development use.

## Integration Policy

Security behavior is owned by the host application.
If your app uses Spring Security, you can either use the default auto-permit helper
or opt out and configure the rule yourself.

### Option A: Default auto permit (quick start)

```yaml
thymeleaflet:
  security:
    auto-permit: true
```

This registers a minimal chain for `/thymeleaflet/**` only.
Use this only for local development or trusted internal environments. If a
`prod` or `production` profile is active, Thymeleaflet logs a warning when
`auto-permit` is enabled. This helper is enabled by default unless set to `false`.

### Option B: Opt out and use an app-side explicit rule

```yaml
thymeleaflet:
  security:
    auto-permit: false
```

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/thymeleaflet/**").permitAll()
    .anyRequest().authenticated()
);
```

## Behavior

- The auto-permit helper adds only `/thymeleaflet/**` authorization.
- The auto-permit helper disables CSRF for the Thymeleaflet UI path so custom story POST rendering works.
- Thymeleaflet adds no header or session rules.
- Existing app security configuration remains authoritative.
- If `auto-permit=false`, Thymeleaflet adds no `SecurityFilterChain`.

## Recommendations

- Restrict `/thymeleaflet/**` in production as needed.
- Use reverse proxy or IP restrictions for internal environments.
- Disable Thymeleaflet in production when not needed.

```yaml
# application-prod.yml
thymeleaflet:
  enabled: false
```

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## Related

- Configuration: [configuration.md](configuration.md)
