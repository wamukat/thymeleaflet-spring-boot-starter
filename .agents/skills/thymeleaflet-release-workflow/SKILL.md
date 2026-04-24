---
name: thymeleaflet-release-workflow
description: Thymeleaflet Spring Boot Starter release workflow. Use when releasing /Users/takuma/workspace/thymeleaflet-spring-boot-starter, preparing a release branch, updating versions/CHANGELOG, creating release PRs, merging release PRs, tagging, publishing to Maven Central, or creating GitHub Releases.
---

# Thymeleaflet Release Workflow

Use this skill for releases of `/Users/takuma/workspace/thymeleaflet-spring-boot-starter`.

## Core Rules

- Follow the repository `AGENTS.md` release flow: never release directly from an implementation branch.
- Create a `release/x.y.z` branch from a clean, up-to-date local `main`.
- Do not squash release PRs unless the user explicitly requests squash.
- Prefer a merge commit for release PRs so local `main`, `origin/main`, and the tag ancestry remain obvious.
- Do not tag from a detached HEAD unless recovering from an explicit, documented failure.
- After merging a release PR, update local `main` to exactly match `origin/main` before tagging or publishing.
- If local `main` and `origin/main` diverge, stop release work and reconcile first. Do not continue tag/publish work while main is divergent.
- If a merge strategy unexpectedly rewrites the commit shape, stop and ask before resetting or discarding local commits.

## Release Workflow

1. Preflight.
   - Read `AGENTS.md` and `RELEASE.md` before planning release mutations.
   - Confirm the release version with the user if not already explicit.
   - Run `git fetch origin`.
   - Run `git status -sb`.
   - Switch to `main`.
   - Ensure local `main` is clean and aligned with `origin/main` before branching.
   - If `main` is behind `origin/main`, fast-forward before branching.
   - If `main` is ahead of or diverged from `origin/main`, stop and explain; do not branch.

2. Create release branch.
   - Create `release/<version>` from `main`.
   - Update only release surfaces:
     - root `pom.xml`
     - `sample/pom.xml`
     - `README.md`
     - `README.ja.md`
     - `CHANGELOG.md`
   - In this skill, "README files" always means both `README.md` and `README.ja.md`.
   - In both README files, update every dependency example version for `io.github.wamukat:thymeleaflet-spring-boot-starter`, including:
     - Maven `<version>x.y.z</version>`
     - Gradle `implementation("io.github.wamukat:thymeleaflet-spring-boot-starter:x.y.z")`
   - Before writing `CHANGELOG.md` and GitHub Release notes, collect actual changes from the previous tag to `main`/release branch using `git log` and available PR/issue context. Do not invent user impact without source changes.
   - Keep changelog entries user-facing and categorized (`Added`, `Changed`, `Fixed`, `Docs`, `Test`, `Build`, etc.).

3. Verify release candidate.
   - Run `./mvnw test -q`.
   - Run `npm run test:e2e:local`.
   - Confirm no sample app remains listening on port `6006`.
   - Run release build checks before publishing when appropriate.

4. Commit and open PR.
   - Commit the release preparation on `release/<version>`.
   - Push `release/<version>`.
   - Open a PR targeting `main`.
   - PR body must use real LF newlines and mention `Closes: N/A` when there is no GitHub issue to close.

5. Merge PR.
   - Re-check the PR is mergeable.
   - Merge the release PR with a merge commit by default.
   - Prefer `gh pr merge <PR> --merge` or the GitHub UI "Create a merge commit" option.
   - Do not use squash by default. Squash causes local main divergence when the release branch contains prior implementation commits.

6. Synchronize local main before tag.
   - Run `git fetch origin`.
   - Switch to `main`.
   - Bring local `main` to `origin/main` using a normal fast-forward update when possible.
   - Preferred commands:
     - `git switch main`
     - `git merge --ff-only origin/main`
   - Verify `git status -sb` shows no ahead/behind/diverged state.
   - Verify `git log --oneline --decorate -3` shows `HEAD -> main` and `origin/main` on the same commit.
   - Only after this, create and push `v<version>`.

7. Publish.
   - Stay on the clean `main` commit containing the tag; do not switch to the old release branch and do not use detached HEAD as a workaround.
   - Run `./mvnw -Prelease clean deploy`.
   - If sandbox blocks GPG or network, rerun with escalation and record the reason.
   - If Central reports manual publishing required, report the deployment ID and Central URL; do not claim Maven Central publishing is complete.

8. GitHub Release.
   - Create or update GitHub Release `v<version>`.
   - Include user-facing notes and verification.
   - Include Maven Central deployment status accurately: uploaded/validated/manual publish required/published.

9. Finish.
   - Report:
     - release PR URL
     - merge commit
     - tag
     - GitHub Release URL
     - Maven Central deployment ID/status
     - verification commands/results
     - any manual step remaining
   - End on a clean local `main` aligned with `origin/main`.

## Divergence Recovery

If local `main` diverges from `origin/main` during release:

1. Stop tag/publish work.
2. Explain the cause clearly.
3. Create a backup branch from local `main`.
   - Use `main-before-<version>-squash-reconcile` unless that name already exists.
4. Ask before destructive reconciliation unless the user already approved it.
5. After approval, align `main` to `origin/main`.
   - Preferred commands after approval:
     - `git switch main`
     - `git reset --hard origin/main`
6. Verify `git status -sb` shows `main...origin/main` with no ahead/behind state.

Never present divergence as normal or harmless release state.
