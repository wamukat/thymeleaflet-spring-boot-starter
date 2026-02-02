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

4) Run the release build:

```bash
./mvnw -Prelease clean deploy
```

5) Verify that the staging repository is closed and released in Sonatype (auto-release enabled).
6) Check Maven Central for the published artifacts.

## Snapshot Deploy

```bash
./mvnw clean deploy
```

## Notes

- This project uses the `release` Maven profile to attach sources, javadocs, and sign artifacts.
- If your OSSRH host is not `s01`, update `distributionManagement` and the Nexus URL in `pom.xml`.
