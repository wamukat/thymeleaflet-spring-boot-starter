# Changelog

All notable changes to this project will be documented in this file.
This project follows the Keep a Changelog format and uses Semantic Versioning.

## [Unreleased]

- Added
  - TBD

## [0.2.23] - 2026-05-03

- Added
  - Added a fragment syntax support matrix in English and Japanese, classifying supported, diagnostic-only, unsupported, and planned Thymeleaf fragment syntax. (Kanbalone #523)
  - Added parser diagnostic carriers and story alert support for multiple parser diagnostics, so malformed template/story syntax can surface more than one user-safe issue. (Kanbalone #502, #527)
- Changed
  - Replaced broad fragment-expression regex handling with shared parsers for fragment references, fragment signatures, and top-level syntax scanning. This improves quoted selectors, nested parameters, same-template references, and no-argument fragment selector handling. (Kanbalone #499, #505, #506, #508, #526)
  - Replaced JavaDoc field regex parsing with structured tag/example parsing while preserving existing story documentation behavior. (Kanbalone #501)
  - Centralized fragment reference attribute policy and injected structured parsers into fragment discovery to keep dependency extraction, fragment discovery, and unsafe insertion checks on the same parser path. (Kanbalone #524, #525)
  - Improved template model inference for selector-style fragment references, named fragment arguments, and stable bracket expressions such as indexed list paths and literal map keys while keeping dynamic keys conservative. (Kanbalone #530, #531, #533)
- Fixed
  - Fixed Central deployment ID extraction when Central publish logs contain noisy manual-publish output. (Kanbalone #497)
  - Added non-fatal diagnostics for duplicate fragment declaration parameters while preserving declaration order. (Kanbalone #532)
- Docs
  - Updated template parsing documentation to describe selector-style fragment references, named argument inference, duplicate parameter diagnostics, and stable bracket expression inference. (Kanbalone #523, #530, #531, #532, #533)
- Test
  - Added parser fixture corpus, structured parser edge corpus, parser comparison contracts, external HTML parser adapter evaluation, and regression coverage for fragment syntax parsing and model inference behavior. (Kanbalone #498, #500, #503, #528)
- Build
  - Updated Maven project, sample app, and README dependency examples to `0.2.23`.

### Issues

- Kanbalone #497 Fix Central deployment id extraction
- Kanbalone #498 Parser fixture corpus
- Kanbalone #499 Shared fragment expression parser
- Kanbalone #500 Structured parser edge corpus
- Kanbalone #501 JavaDoc tag parser
- Kanbalone #502 Parser diagnostics carriers
- Kanbalone #503 External HTML parser adapter evaluation
- Kanbalone #505 Dependency fragment signature parser
- Kanbalone #506 No-arg fragment selector normalizer
- Kanbalone #508 Top-level syntax scanner
- Kanbalone #523 Fragment syntax support matrix
- Kanbalone #524 Fragment discovery parser injection
- Kanbalone #525 Fragment reference attribute policy
- Kanbalone #526 Same-template fragment references
- Kanbalone #527 Multiple parser diagnostics
- Kanbalone #528 Parser comparison test support
- Kanbalone #530 Selector-style fragment references
- Kanbalone #531 Named fragment argument mapping
- Kanbalone #532 Duplicate fragment parameter diagnostics
- Kanbalone #533 Stable bracket expression inference
- PR #158 Fix Central deployment id extraction
- PR #159 Expand parser fixture corpus
- PR #160 Introduce shared fragment expression parser
- PR #161 Expand structured parser edge corpus
- PR #162 Replace JavaDoc field regex parsing
- PR #163 Add parser diagnostics carriers
- PR #164 Evaluate external HTML parser adapter
- PR #165 Use signature parser for dependency fragment matching
- PR #166 Scan no-arg fragment selectors without broad regex replacement
- PR #167 Use structured attributes for unsafe fragment insertion checks
- PR #168 Extract top-level syntax scanner
- PR #169 Surface parser diagnostics in story diagnostics
- PR #170 Extract parser comparison test contract
- PR #171 Document fragment syntax support matrix
- PR #172 Inject parsers into fragment discovery service
- PR #173 Centralize fragment reference attribute policy
- PR #174 Support same-template fragment references
- PR #175 Support multiple parser diagnostics in story alerts
- PR #176 Harden parser comparison test support
- PR #177 Support selector-style fragment references
- PR #178 Map named fragment arguments for inference
- PR #179 Diagnose duplicate fragment parameters
- PR #180 Infer stable bracket expression paths

## [0.2.22] - 2026-05-02

- Added
  - Added a release-log helper, `npm run release:central-status`, that summarizes Maven Central deployment ID and status as `uploaded`, `validated`, `manual-publish-required`, or `published` for accurate release reporting. (Kanbalone #490)
- Changed
  - Migrated fragment dependency extraction to structured template parsing, including `data-th-*` replacement/include/insert attributes, quoted template paths, and subtree scoping for target fragments. (Kanbalone #486)
  - Migrated fragment definition parsing to the shared structured template parser, including `data-th-fragment` support. (Kanbalone #487)
  - Replaced the outer Thymeleaf expression regex in model inference with a quote/brace-aware scanner, improving handling of literal braces, malformed expressions, and selection expressions. (Kanbalone #488)
- Fixed
  - Preserved multiline `@param` and `@model` JavaDoc descriptions while splitting JavaDoc parsing into focused comment-block, tag, and example parser collaborators. (Kanbalone #489)
- Docs
  - Updated the Maven Central release guide to retain deploy logs, run the Central status helper, and report manual-publish-required deployments without claiming Maven Central publishing is complete. (Kanbalone #490)
- Test
  - Added regression coverage for structured fragment dependencies and definitions, Thymeleaf expression scanning edge cases, JavaDoc parsing blocks, and Central publish status parsing. (Kanbalone #486, #487, #488, #489, #490)
- Build
  - Updated Maven project, sample app, and README dependency examples to `0.2.22`.

### Issues

- Kanbalone #486 Migrate fragment dependency extraction to structured template parsing
- Kanbalone #487 Migrate fragment definition parsing to structured template parsing
- Kanbalone #488 Replace outer Thymeleaf expression extraction regex with tokenizer/parser
- Kanbalone #489 Split JavaDocAnalyzer into focused parsers with regression coverage
- Kanbalone #490 Add release helper for Central manual-publish status reporting
- PR #155 Refactor structured template parsing hotspots
- PR #156 Refactor JavaDoc parsing and release status reporting

## [0.2.21] - 2026-05-02

- Changed
  - Replaced fragile regex-only template attribute extraction with a structured parser abstraction backed by Thymeleaf-compatible parsing, then migrated model inference and JavaDoc analysis to the shared parser path. (Kanbalone #479, #480)
  - Added a Thymeleaf expression tokenizer for model path inference, improving handling of utility calls, static class paths, aliases, reserved words, and unsupported dynamic bracket expressions. (Kanbalone #481)
- Fixed
  - Applied Java time Story YAML conversion to nested list model paths such as `view.someObject.items[].publishedAt`, so `#temporals.format` can render nested collection values from story data. (#153)
- Docs
  - Documented the template parsing architecture, supported syntax, and parser tradeoffs in English and Japanese documentation. (Kanbalone #483)
- Test
  - Added parser regression fixtures based on recent defects, including no-arg fragment references, quoted selectors, nested Java time paths, and child model inference. (Kanbalone #482)
  - Added direct story diagnostic mapping tests plus template rendering coverage to ensure `code` and `userSafeMessage` are shown while `developerMessage` remains hidden. (Kanbalone #211, #212)
  - Added local E2E coverage for malformed `.stories.yml` files showing a user-safe Story YAML diagnostic without changing existing snapshot baselines. (Kanbalone #213)
- Build
  - Updated Maven project, sample app, and README dependency examples to `0.2.21`.

### Issues

- #153 `#148` fix does not apply to nested list paths such as `view.someObject.items[].publishedAt`
- Kanbalone #211 Story diagnostics mapping direct tests
- Kanbalone #212 Story diagnostics template display tests
- Kanbalone #213 Broken `.stories.yml` UI diagnostic E2E
- Kanbalone #477 Roadmap: replace fragile template parsing with structured parsing
- Kanbalone #479 Structured template parser abstraction
- Kanbalone #480 Template model analyzer structured extraction migration
- Kanbalone #481 Thymeleaf expression tokenizer for model path inference
- Kanbalone #482 Parser regression fixture corpus
- Kanbalone #483 Template parsing architecture documentation

## [0.2.20] - 2026-05-02

- Fixed
  - Normalized no-argument fragment references such as `topbar()` to `topbar` during Thymeleaflet preview rendering, allowing templates that use Spring MVC-compatible no-arg fragment call syntax to render without preview-only fragment resolution failures. (#149)
  - Preserved Story YAML Java time model conversion regression coverage for `@model` list paths such as `view.items[].publishedAt`, confirming YAML ISO strings continue to render through `#temporals.format`. (#148)
- Test
  - Added preview regression coverage for no-arg fragment references declared without parameters and referenced with `name()`.
  - Added unit coverage for quoted no-arg selectors and for leaving positional/named parameterized fragment references unchanged.
- Build
  - Updated Maven project, sample app, and README dependency examples to `0.2.20`.

### Issues

- #148 ストーリーデータが String 型の場合、#temporals.format() が EL1004E で失敗する
- #149 パラメータなしフラグメントを topbar() 形式で参照するとプレビューで解決失敗する

## [0.2.19] - 2026-05-01

- Added
  - Added GitHub Actions verification for pull requests and pushes to `main`, including Maven tests and local E2E coverage. (Kanbalone #462)
- Changed
  - Reduced normal Thymeleaflet runtime success-path logging from INFO to DEBUG while keeping warning/error diagnostics visible. (Kanbalone #455)
  - Centralized runtime-derived cache handling for templates, JavaDoc, type information, fragment discovery, and dependencies behind a shared cache manager. (Kanbalone #456)
  - Split template scanning and fragment definition parsing into focused components while preserving existing fragment discovery behavior. (Kanbalone #458)
- Fixed
  - Made `thymeleaflet.enabled=false` effective for auto-configuration and added production-profile diagnostics for active Thymeleaflet exposure. (Kanbalone #459)
  - Applied `thymeleaflet.resources.cache-duration-seconds` to Thymeleaflet static resource handlers so configuration metadata matches runtime behavior. (Kanbalone #460)
  - Cleaned up local E2E helper shutdown output so expected sample-app termination no longer appears as a Maven build failure. (Kanbalone #461)
  - Converted Story YAML ISO string values to Java time types when JavaDoc declares matching `@param` or nested `@model` paths, including list wildcard paths such as `view.items[].publishedAt`. (#137)
- Docs
  - Expanded configuration metadata documentation and production exposure guidance.
  - Documented supported Java time Story YAML values in English and Japanese story docs.
- Build
  - Updated Maven project, sample app, and README dependency examples to `0.2.19`.
- Test
  - Added regression coverage for cache management, classloader template resolution, scanner/parser separation, production exposure diagnostics, configuration metadata, local E2E helper shutdown handling, GitHub Actions workflow shape, and Java time story rendering.

### Issues

- #137 `story` の `model` / `parameters` で `java.time` 型フィールドが `String` として渡され `#temporals.format` がエラーになる
- Kanbalone #455 Thymeleaflet runtime log noise
- Kanbalone #456 Centralize Thymeleaflet cache management
- Kanbalone #457 Add classloader template resolver integration coverage
- Kanbalone #458 Separate template scanning and fragment parsing
- Kanbalone #459 Harden Thymeleaflet production exposure controls
- Kanbalone #460 Expand Spring configuration metadata
- Kanbalone #461 Clean up local E2E shutdown output
- Kanbalone #462 Add GitHub Actions verification workflow

## [0.2.18] - 2026-05-01

- Added
  - Added explicit `@fragment <name>` JavaDoc binding so localized descriptions can be matched to fragments without requiring the fragment name to appear in the text. (#131)
- Changed
  - When `spring.thymeleaf.cache=false` and `thymeleaflet.cache.enabled` is not explicitly set, Thymeleaflet now disables its internal JavaDoc/template caches during development so template-only edits are reflected on reload. (#132)
- Fixed
  - Expanded `@example` parsing to support `<th:block th:replace="...">` and no-argument fragment references without parentheses. (#130)
- Docs
  - Documented the new `@fragment` JavaDoc tag and development cache behavior in both English and Japanese README files.
- Test
  - Added regression coverage for `@fragment` lookup, localized JavaDoc descriptions, `th:block` examples, no-argument examples, and disabled JavaDoc template caching.
- Build
  - Updated Maven project and sample app versions to `0.2.18`.

### Issues

- #130 `@example` が `<th:block>` や引数なしフラグメントで解析されない
- #131 日本語（非ASCII）の JavaDoc 説明文では matchesDescription が機能しない
- #132 Spring Boot DevTools 環境でテンプレート変更後に JavaDoc キャッシュが更新されない

## [0.2.17] - 2026-05-01

- Fixed
  - Added a high-priority `ClassLoaderTemplateResolver` for `thymeleaflet/**` templates so Thymeleaflet views packaged in the starter JAR resolve correctly when Spring Boot DevTools splits application and dependency class loaders. (#129)
- Docs
  - Added the repository release workflow skill documentation used by agents working on this project.
- Test
  - Added regression coverage for the Thymeleaflet class loader template resolver configuration.
- Build
  - Updated Maven project and sample app versions to `0.2.17`.

### Issues

- #129 Spring Boot DevTools 環境で JAR 内テンプレートが解決できない

## [0.2.16] - 2026-04-25

- Added
  - Added Story YAML load diagnostics so malformed story files can be distinguished from missing story configuration.
  - Surfaced story configuration diagnostics in the fragment main content and story preview UI with user-safe messages.
- Changed
  - Converted controller and service wiring to constructor injection for clearer dependencies and easier testing.
- Docs
  - Updated README dependency examples to the current release version.
  - Documented the local E2E verification flow.
- Test
  - Added README dependency version consistency coverage.
- Build
  - Added a local E2E helper command that installs the starter, starts the sample app, runs Playwright, and stops the sample process.
  - Updated Maven project and sample app versions to `0.2.16`.

### Issues

- #188 Fragment/story 診断を UI に表示する
- #189 Service 層を constructor injection に統一する
- #190 ローカル E2E を 1 コマンドで再現できるようにする
- #191 README の依存バージョンを自動検査する
- #192 Story YAML の読み込み失敗を診断情報として扱う

## [0.2.15] - 2026-03-03

- Fixed
  - Added explicit Java Time support for the starter default `ObjectMapper` by registering `JavaTimeModule`, preventing serialization issues for `LocalDateTime` and related `java.time` types.
  - Switched default date/time JSON serialization from timestamp form to ISO-8601 text output for predictable preview/model JSON behavior.
- Test
  - Added regression coverage to ensure the starter default `ObjectMapper` serializes `LocalDateTime` as ISO-8601 text.
- Build
  - Added `jackson-datatype-jsr310` dependency and updated Maven project/sample app versions to `0.2.15`.

### Issues

- #124 Fix default ObjectMapper Java Time support

## [0.2.14] - 2026-03-02

- Fixed
  - Replaced direct `WebMvcAutoConfiguration` type reference in `StorybookAutoConfiguration` with class-name based ordering so startup works on both Spring Boot 3 and 4.
  - Added a conditional fallback `ObjectMapper` bean in auto-configuration so Boot 4 startup does not fail when no `com.fasterxml.jackson.databind.ObjectMapper` is auto-registered.
  - Updated `WebConfig` path-matching setup to skip removed `PathMatchConfigurer` legacy setters on Spring Framework 7 (Boot 4), preventing startup `NoSuchMethodError`.
  - Updated `CookieLocaleResolver` registration to use constructor-based cookie name and `Duration` max-age so locale resolver wiring works on both Spring Framework 6 and 7.
- Docs
  - Updated requirements docs to indicate Spring Boot `3.1+ / 4.x` support.
- Build
  - Updated Maven project version and sample app dependency to `0.2.14`.

### Issues

- #120 Fix Spring Boot 4 compatibility in starter auto-configuration

## [0.2.13] - 2026-02-26

- Added
  - Added no-arg method-return candidate extraction (`a.b.c()`) so stories can configure method-style values separately from plain model paths.
  - Added preview-time fallback components to resolve no-arg method calls from map-backed model values and record warnings.
- Changed
  - Split method returns and model values in the story editor UI (including standard stories), and updated panel layout/placement for custom story editing.
  - Improved sample value inference heuristics for datetime-like key suffixes (`*At`, `*_at`, `*-at`, `*datetime*`, `*timestamp*`).
- Fixed
  - Fixed preview rendering failures for map-backed no-arg method expressions (for example `view.pointPage.hasPrev()` / `hasNext()`).
  - Fixed preview rendering failures caused by string-inferred datetime/boolean fields (for example `publishedAt` with `#temporals.format(...)` and `read` ternary conditions).
- Test
  - Added regression tests for analyzer/path separation, story persistence, rendering fallback/warning behavior, and datetime/boolean sample inference.
- Docs
  - Updated EN/JA stories documentation to reflect method-return handling and configuration behavior.
- Build
  - Updated Maven project version and sample app dependency to `0.2.13`.

### Issues

- #115 Fix sample inference for datetime-like keys in preview

## [0.2.12] - 2026-02-23

- Fixed
  - Eliminated startup WARN from `PostProcessorRegistrationDelegate$BeanPostProcessorChecker` by declaring `thymeleafletMessageSourcePostProcessor` as a static `@Bean` factory method in `StorybookAutoConfiguration`.
- Test
  - Added regression coverage to ensure `thymeleafletMessageSourcePostProcessor` remains a static bean factory method.
- Build
  - Updated Maven project version and sample app dependency to `0.2.12`.

### Issues

- #112 Fix BeanPostProcessorChecker WARN on startup

## [0.2.11] - 2026-02-12

- Fixed
  - Prevented fallback/custom model inference from leaking child fragment model keys into parent fragments when `th:replace`/`th:insert` references pass literal-only arguments.
  - Kept recursive merge for static references that have no arguments or dynamic/non-literal arguments.
- Test
  - Added regression coverage for recursion-flag extraction in template reference analysis and for literal-only child reference merge suppression in model inference service.
- Build
  - Updated Maven project version and sample app dependency to `0.2.11`.

### Issues

- #99 Fix fallback model inference and source snippet truncation

## [0.2.10] - 2026-02-12

- Fixed
  - Fixed architecture rule violation by removing Spring stereotype annotation from `TemplateModelExpressionAnalyzer` in the `domain` package and wiring it from auto-configuration.
  - Fixed mobile top-page welcome view after HTMX/main-content replacement by restoring the sidebar open (hamburger) button in the welcome block.
- Test
  - Re-ran targeted mobile sidebar E2E regression to confirm open-button visibility and sidebar reopening behavior.
- Build
  - Updated Maven project version and sample app dependency to `0.2.10`.

### Issues

- #103 Fix architecture test failure for domain analyzer bean
- #105 Fix missing mobile hamburger button in welcome main-content

## [0.2.9] - 2026-02-12

- Added
  - Added fallback story model inference support for statically referenced fragments (`th:replace` / `th:insert`) so required model keys can be merged from child fragments.
- Changed
  - Improved fallback/custom model defaults around message-like fields by treating `*message*` keys as string samples.
  - Improved mobile top-page navigation by ensuring the sidebar open (hamburger) button is consistently visible in placeholder/welcome states.
- Refactored
  - Refactored model inference into richer domain objects (`ModelPath`, `TemplateInference`, `InferredModel`) and reduced orchestration logic in web service layer.
  - Extracted/organized template expression analysis responsibilities to improve maintainability and testability.
- Fixed
  - Fixed fallback rendering failure for fragments that depend on nested panel fragment requirements (e.g. `pointsContent` including `pointsPanel`).
  - Fixed source implementation snippet extraction so fragment code blocks are not truncated mid-element.
- Test
  - Added regression tests for loop/collection model inference, static fragment reference merging, source-snippet extraction, and message-field sample typing.
  - Re-ran E2E regression suite for mobile sidebar and preview flows.
- Build
  - Updated Maven project version and sample app dependency to `0.2.9`.

### Issues

- #95 Bug: mobile view cannot reopen fragment list sidebar
- #99 Fix fallback model inference and source snippet truncation

## [0.2.8] - 2026-02-11

- Added
  - Added explicit custom-parameter type switching (`string` / `number` / `boolean` / `object` / `array`) in Custom story editing.
  - Added NULL toggle support for custom parameters with value restoration behavior when toggling back off.
  - Added custom-parameter reset action to clear session-saved custom values and restore defaults from the first/default story.
- Changed
  - Improved fallback inference for collection/map-like parameters in `StoryParameterDomainService` with parameter-name-aware defaults.
  - Aligned Custom parameter controls and reset icon placement to improve editing flow in the parameter panel.
  - Updated mobile navigation UX so the sidebar open button is available beside the fragment title without reserving extra header space.
  - Made mobile sidebar button placement consistent between initial page render and HTMX-swapped main content.
- Fixed
  - Isolated Thymeleaflet message bundles from host application `messages*.properties` to prevent i18n key collisions.
  - Guarded unsafe fragment insertion parameter patterns (`th:replace` / `th:insert` with plain strings) to avoid preview-time template resolution failures.
  - Restored mobile fragment-list accessibility so users can reliably reopen the sidebar after closing it.
  - Fixed fallback-story badge handling and source-panel rendering issues (including signature/implementation display refinements).
- Docs
  - Expanded EN/JA documentation for safe fragment insertion patterns and story parameter design guidance.
  - Updated EN/JA stories docs to explain type inference/fallback behavior and boundaries.
- Test
  - Added regression tests for fallback inference behavior (`options`/list/map paths).
  - Added/updated integration coverage for i18n isolation and rendering-exception guard behavior.
  - Added mobile sidebar open/close E2E regression coverage and re-ran full E2E suite.
- Build
  - Updated Maven project version and sample app dependency to `0.2.8`.

### Issues

- #88 Fix message bundle basename collision with consumer apps
- #90 Guard unsafe fragment insertion parameters and improve guidance
- #92 Fix collection-like fallback inference for custom story params
- #95 Bug: mobile view cannot reopen fragment list sidebar

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
