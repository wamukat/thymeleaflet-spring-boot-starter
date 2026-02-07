# Changelog

All notable changes to this project will be documented in this file.
This project follows the Keep a Changelog format and uses Semantic Versioning.

## [Unreleased]

- TBD

## [0.2.3] - 2026-02-07

- Complete parser migration planning and implementation (spec/model docs, parser integration, diagnostics, UI alignment).
- Define Thymeleaf 3.1.2-based signature policy and clarify declaration-side unsupported syntax handling.
- Add structured fragment signature diagnostics and surface user-safe diagnostic messages in UI.
- Remove legacy plain-name fallback for invalid fragment signatures.
- Align Story Values ordering and state rendering across main content and story preview.

### Issues

- #39 Epic: Thymeleaf Fragment Parser の全面刷新（Regex廃止）
- #48 Parser仕様策定: サポート対象文法の定義
- #49 新ドメインモデル設計: Fragment解析結果の再定義
- #50 Parser実装(1): th:fragmentシグネチャ解析
- #51 Parser実装(2): JavaDoc連携とParamマッピング
- #52 診断改善: Parserエラー分類と表示方針
- #53 UI接続: Fragment Details / Story values の新モデル対応
- #54 負債返済: 旧Regex解析経路の撤去
- #55 ドキュメント更新: 新Parser仕様・互換性・制約

## [0.2.2] - 2026-02-07

- Simplify Usage panel by removing model-specific helper text and model value blocks.
- Align story value ordering with JavaDoc parameter specification order when available.
- Remove redundant "Fragment parameters" label from Fragment Details parameter spec section.
- Improve YAML preview modal rendering stability to avoid initial flicker.

### Issues

- #35 UI minor adjustments for preview and story values
- #37 Refine Usage panel and fragment details labels

## [0.2.1] - 2026-02-06

- Switch viewport controls to width-only presets and remove rotate/ruler-related UI.
- Fix fullscreen and fit preview sizing behavior so fragments render without clipping.
- Refactor preview controls/templates and strengthen E2E coverage.
- Refresh English/Japanese documentation and screenshots for the updated preview behavior.

### Issues

- #25 Fullscreen preview clipping fix
- #31 Viewport width-only update and docs refresh

## [0.2.0] - 2026-02-06

- Make preview viewport presets and preview background colors configurable.
- Add fullscreen preview mode and responsive header sample for layout verification.
- Refine preview viewport sizing, fit behavior, and labels.

### Issues

- #7 Viewport presets: switch preview size

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
