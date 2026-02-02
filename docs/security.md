# Security

Thymeleaflet includes a dedicated security filter chain for `/thymeleaflet/**`.
By default, it is enabled.
This tool is intended for development use.

## Configuration

```yaml
thymeleaflet:
  security:
    enabled: true
```

## Behavior (Default)

- All `/thymeleaflet/**` endpoints are permitted (for development).
- CSRF protection is enabled with cookie-based tokens.
- Several endpoints are excluded from CSRF (HTMX preview/content).
- Security headers (HSTS, CSP, Referrer-Policy, Permissions-Policy) are set.

## Recommendations

- Disable or restrict `/thymeleaflet/**` in production.
- Use a reverse proxy or IP restriction for internal access.
- If you need custom security rules, disable and provide your own chain.

To disable in production, exclude the auto-configuration:

```yaml
# application-prod.yml
spring:
  autoconfigure:
    exclude: io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookAutoConfiguration
```

## Related

- Configuration: [configuration.md](configuration.md)
