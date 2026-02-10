# Changelog

All notable changes to this project will be documented in this file.
This project follows the Keep a Changelog format and uses Semantic Versioning.

## [Unreleased]

- Added
  - TBD

## [0.2.7] - 2026-02-11

- Changed
  - Refined Spring Security integration to avoid host-application side effects by default.
  - Added opt-in quick-start helper `thymeleaflet.security.auto-permit=true` for Spring Security users who want automatic `/thymeleaflet/**` permit setup in development.
- Removed
  - Removed built-in Thymeleaflet security filter chain defaults that could unintentionally affect existing application security behavior.
- Test
  - Verified configuration binding/resolution for the new `security.auto-permit` option and re-ran E2E regression suite.
- Docs
  - Updated English/Japanese security and configuration guides with two integration paths: opt-in auto-permit and explicit app-side security rule.
- Build
  - Updated Maven project version and sample app dependency to `0.2.7`.

### Issues

- #84 Avoid unexpected Spring Security impact from starter dependency

## [0.2.6] - 2026-02-09

- Changed
  - Unified base-path handling across configuration and security integration paths.
  - Switched core web service wiring to constructor injection for clearer dependency boundaries.
- Fixed
  - Added fail-fast validation for `thymeleaflet.base-path`; only `/thymeleaflet` is supported and unsupported values now fail at startup with a clear message.
- Test
  - Adapted service tests to constructor injection and added coverage for base-path normalization/rejection behavior.
- Docs
  - Aligned story path / naming guidance and Java build requirement notes in README docs.
  - Clarified fixed base-path limitation (`/thymeleaflet`) in configuration/getting-started docs.
  - Updated installation version examples in `README.md` / `README.ja.md` to `0.2.6`.
- Build
  - Updated Maven project version and sample app dependency to `0.2.6`.

### Issues

- #79 Review fixes for base-path and DI updates
- #80 Fail fast when thymeleaflet.base-path is not /thymeleaflet

## [0.2.5] - 2026-02-08

- Fixed
  - Aligned source-build and demo image Java requirements to avoid deployment build failures on Error Prone/NullAway-enabled compilation.
  - Resolved `ResolvedStorybookConfig` nullable nested-config handling issues flagged by NullAway.
- Refactored
  - Introduced and applied architecture boundary tests (ArchUnit) for ports, domain purity, and configuration-boundary usage.
  - Reduced direct infrastructure coupling in application services by introducing fragment catalog abstraction where effective.
  - Balanced architecture strictness with developer experience by avoiding over-abstraction in UI model coordination paths.
- Test
  - Added integration coverage for story-parameter retrieval behavior to protect refactoring safety.
- Docs
  - Added `ARCHITECTURE.md` to explicitly document currently enforced architecture rules and operational policy.
  - Updated `AGENTS.md` with release guardrails (release branch flow, release checklist, and Java build/runtime notes).
- Build
  - Updated Maven project version and sample app dependency to `0.2.5`.

### Issues

- #73 ArchUnit設計制約の追加と運用ポリシー整備

## [0.2.4] - 2026-02-08

- Changed
  - Standardized issue labeling to `type:*` taxonomy to streamline release-note grouping.
- Refactored
  - Expanded null-safety migration by replacing nullable flows with explicit non-null contracts and `Optional`-based APIs across domain/application/infrastructure layers.
  - Introduced resolved runtime configuration (`ResolvedStorybookConfig`) with fail-fast validation, separating raw property binding from validated config usage.
- Build
  - Enabled Error Prone + NullAway checks and expanded analysis scope to project packages.
- Test
  - Added usage restriction test to keep `StorybookProperties` confined to configuration boundaries.
- Docs
  - Standardized changelog structure to Keep a Changelog-style categorized entries and documented label policy.

### Issues

- #65 NullAway導入(Phase 1): jspecify + discoveryパッケージ適用
- #63 Release 0.2.3

## [0.2.3] - 2026-02-07

- Added
  - Structured fragment signature diagnostics and user-safe diagnostic messages in UI.
- Changed
  - Fragment signature handling to Thymeleaf 3.1.2-based declaration-side policy.
  - Story Values ordering/state rendering alignment between main content and story preview.
- Refactored
  - End-to-end parser migration (spec/model docs, parser integration, diagnostics wiring, UI alignment).
- Removed
  - Legacy plain-name fallback for invalid fragment signatures.
- Docs
  - Parser specification and declaration-side unsupported syntax clarification.

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

- Changed
  - Usage panel simplification by removing model-specific helper text/model value blocks.
  - Story value ordering to follow JavaDoc parameter specification order when available.
  - Fragment Details parameter spec section by removing redundant "Fragment parameters" label.
- Fixed
  - YAML preview modal initial flicker.

### Issues

- #35 UI minor adjustments for preview and story values
- #37 Refine Usage panel and fragment details labels

## [0.2.1] - 2026-02-06

- Changed
  - Viewport controls to width-only presets and removed rotate/ruler-related UI.
- Fixed
  - Fullscreen/Fit preview sizing so fragments render without clipping.
- Refactored
  - Preview controls/templates.
- Test
  - Strengthened E2E coverage for preview behavior.
- Docs
  - Refreshed English/Japanese docs and screenshots for updated preview behavior.

### Issues

- #25 Fullscreen preview clipping fix
- #31 Viewport width-only update and docs refresh

## [0.2.0] - 2026-02-06

- Added
  - Configurable preview viewport presets and preview background colors.
  - Fullscreen preview mode and responsive header sample for layout verification.
- Changed
  - Preview viewport sizing, fit behavior, and labels.

### Issues

- #7 Viewport presets: switch preview size

## [0.1.1] - 2026-02-05

- Added
  - Custom model editing support when `stories.yml` is missing.
  - Object literal input support for custom model values.
- Changed
  - Custom badge design to icon-only cues.
- Removed
  - Tracking of generated sample CSS.

### Issues

- #18 Custom story cannot edit model when stories.yml is missing

## [0.1.0] - 2026-02-05

- Added
  - Editable controls for story parameters/models.
- Changed
  - Custom/default story layout alignment.
- Refactored
  - Preview/controls structure and supporting UI fragments.

### Issues

- #11 refactor: split right-pane UI into dedicated fragments
- #10 refactor: expose preview update API for interactive controls
- #6 Controls UI: edit story/model parameters from the UI
- #2 Refactor i18n integration to avoid host conflicts
- #1 Refactor fragment-list UI into smaller templates/modules

## [0.0.2] - 2026-02-03

- Added
  - Preview JS injection via `thymeleaflet.resources.scripts`.
- Changed
  - Preview rendering to iframe for better JS isolation.
  - Preview background sync with container color and same-origin iframe behavior.

## [0.0.1] - 2026-02-02

- Added
  - Initial public release.
