# Security

Thymeleaflet does not register a Spring Security filter chain.
This tool is intended for development use.

## Integration Policy

Security behavior is owned by the host application.
If your app uses Spring Security, you can either use opt-in auto-permit or configure the rule yourself.

### Option A: Opt-in auto permit (quick start)

```yaml
thymeleaflet:
  security:
    auto-permit: true
```

This registers a minimal chain for `/thymeleaflet/**` only.

### Option B: App-side explicit rule

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/thymeleaflet/**").permitAll()
    .anyRequest().authenticated()
);
```

## Behavior

- Thymeleaflet adds no authentication/authorization rules.
- Thymeleaflet adds no CSRF/header/session rules.
- Existing app security configuration remains authoritative.
- If `auto-permit=true`, Thymeleaflet adds only a minimal `/thymeleaflet/**` permit chain.

## Recommendations

- Restrict `/thymeleaflet/**` in production as needed.
- Use reverse proxy or IP restrictions for internal environments.
- Disable Thymeleaflet in production when not needed.

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## Related

- Configuration: [configuration.md](configuration.md)
