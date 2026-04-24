# Contributing

Thanks for your interest in contributing to Thymeleaflet!

## How to Contribute

1) Fork the repository and create a feature branch.
2) Make your changes with clear, focused commits.
3) Ensure tests pass and the UI CSS build succeeds.
4) Open a pull request with a concise description.

## Development Setup

```bash
./mvnw test
npm install
npm run build
```

## Local Verification

For starter changes that affect the sample app or browser behavior, reinstall the
starter locally and run E2E with the local helper:

```bash
npm run test:e2e:local
```

The helper starts the sample app on port `6006`, waits for it, runs Playwright,
and stops the sample process when the command finishes.

To run the steps manually:

```bash
npm run verify:starter
npm run sample:start
```

Then run E2E from another terminal:

```bash
npm run test:e2e
```

## Code Style

- Prefer small, readable changes.
- Keep public API changes well documented.
- Add tests for new behavior when possible.

## Reporting Issues

- Use the issue tracker for bugs and feature requests.
- Provide reproduction steps and environment details.

## License

By contributing, you agree that your contributions will be licensed under the Apache-2.0 License.
See [LICENSE](LICENSE).

## Related

- Project overview: [README.md](README.md)
