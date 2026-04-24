---
name: "thymeleaflet-test-execution"
description: "Run tests for thymeleaflet-spring-boot-starter with a deterministic, low-friction workflow. Use when asked to verify PRs, execute unit/integration/E2E tests, investigate slow or stuck test runs, or report test results with clear recovery steps."
---

# Thymeleaflet Test Execution

## Overview
Execute tests in a fixed order that minimizes wasted time: preflight checks, fastest meaningful tests first, then E2E with explicit app readiness checks. Use fail-fast recovery when runs stall.

## Workflow

### 1. Run preflight checks
1. Confirm current branch and working tree state.
2. Confirm tool availability: `java`, `node`, `npm`.
3. Confirm whether sample app is already running before starting another process.
4. Read `references/command-recipes.md` for standard commands.

### 2. Run fastest meaningful verification first
1. Run focused tests for changed classes when possible.
2. Run `mvn -DskipTests install` when build-level verification is needed.
3. Run E2E last, after app readiness is confirmed.

### 3. Start sample app only when required
1. Start from `sample/` with Maven Wrapper: `../mvnw spring-boot:run -DskipTests`.
2. Prefer wrapper over system Maven to avoid version drift.
3. Wait for startup completion (`Started SampleApplication`) before E2E.

### 4. Execute E2E deterministically
1. Run `npm run test:e2e` from repository root.
2. If runtime is unstable, rerun with single worker and scoped grep pattern.
3. Record pass/fail counts and notable warnings.

### 5. Recover quickly when execution stalls
1. If E2E cannot connect, verify sample app process and port first.
2. If port is occupied by stale process, terminate stale process and restart sample app once.
3. If Maven compile level errors appear, switch to `./mvnw` or `../mvnw`.
4. If `npm` is unavailable, report blocker explicitly and stop E2E attempts.

## Reporting format
1. List commands actually executed.
2. List result summary (`passed`, `failed`, `skipped`).
3. List blockers and concrete next action.

## Notes
- Always run E2E after implementation or fix work.
- After installing updated starter artifacts, restart sample app so changes are reflected.
