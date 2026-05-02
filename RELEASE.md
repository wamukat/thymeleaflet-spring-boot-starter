# Release Guide (Maven Central)

This document describes how to release `thymeleaflet-spring-boot-starter` to Maven Central
via Sonatype OSSRH (s01).

## Prerequisites

- Sonatype OSSRH account (s01)
- Project approved under `io.github.wamukat`
- GPG key created and published to a keyserver
- GitHub repository with tags enabled

## Required Credentials

Configure these in `~/.m2/settings.xml` (not committed):

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>
</settings>
```

GPG signing can be configured via environment variables or your local keyring.

## Release Steps

1) Update the version in `pom.xml` to a release version (remove `-SNAPSHOT`).
2) Update `CHANGELOG.md`.
3) Commit changes and tag the release:

```bash
git tag vX.Y.Z
```

4) Run the release build and keep the full deploy log:

```bash
./mvnw -Prelease clean deploy 2>&1 | tee /tmp/thymeleaflet-release-X.Y.Z.log
```

5) Summarize the Central Publishing deployment status from the log:

```bash
npm run release:central-status -- /tmp/thymeleaflet-release-X.Y.Z.log
```

The helper reports:

- `uploaded`: artifacts were uploaded or a deployment ID was created, but validation/publish status was not found.
- `validated`: Central validation completed, but publish status was not found.
- `manual-publish-required`: Central accepted/validated the deployment but requires manual publishing. Include the deployment ID and https://central.sonatype.com/publishing/deployments in release notes and do not claim Maven Central publishing is complete.
- `published`: deployment output indicates the artifacts were published/released.

6) Verify the deployment in Central Portal. If the helper reports `manual-publish-required`, publish the deployment manually from Central Portal or clearly report the remaining manual step.
7) Check Maven Central for the published artifacts.

## How To Collect Release Changes (Checklist)

Before editing `CHANGELOG.md`, always collect all merged changes since the last tag.

1) Identify the previous release tag:

```bash
git tag --sort=version:refname | tail -n 1
```

2) List commits since the previous tag:

```bash
git log --oneline <PREVIOUS_TAG>..main
```

3) List merged PRs since the previous tag date:

```bash
gh pr list --state merged --base main --search "merged:>=YYYY-MM-DD" --json number,title,mergedAt,headRefName
```

4) For each candidate PR, open summary/details and map to changelog categories:

```bash
gh pr view <PR_NUMBER> --json number,title,body,url,mergedAt
```

5) Update `CHANGELOG.md` categories (`Added` / `Changed` / `Fixed` / `Removed` / `Refactored` / `Docs` / `Build` / `Test`) and include related issue numbers.

This prevents missing important fixes/features in release notes.

## Snapshot Deploy

```bash
./mvnw clean deploy
```

## Notes

- This project uses the `release` Maven profile to attach sources, javadocs, and sign artifacts.
- If your OSSRH host is not `s01`, update `distributionManagement` and the Nexus URL in `pom.xml`.
