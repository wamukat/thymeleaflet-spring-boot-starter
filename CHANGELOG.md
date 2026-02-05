# Changelog

All notable changes to this project will be documented in this file.
This project follows the Keep a Changelog format and uses Semantic Versioning.

## [Unreleased]

- TBD

## [0.1.1] - 2026-02-05

- Allow custom model editing when `stories.yml` is missing.
- Accept object literal input for custom model values.
- Tidy Custom badges to icon-only cues and stop tracking generated sample CSS.

### Issues

- #18 Custom story cannot edit model when stories.yml is missing

## [0.1.0] - 2026-02-05

- Added editable controls for story parameters/models and aligned custom/default story layouts.
- Improved preview/controls structure and supporting UI fragments.

### Issues

- #11 refactor: split right-pane UI into dedicated fragments
- #10 refactor: expose preview update API for interactive controls
- #6 Controls UI: edit story/model parameters from the UI
- #2 Refactor i18n integration to avoid host conflicts
- #1 Refactor fragment-list UI into smaller templates/modules

## [0.0.2] - 2026-02-03

- Switched preview rendering to iframe for better JS isolation
- Added preview JS injection via `thymeleaflet.resources.scripts`
- Synced preview background with container color and same-origin iframe behavior

## [0.0.1] - 2026-02-02

- Initial public release
