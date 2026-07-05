[← Documentation](README.md)

# Roadmap

This document tracks the direction of **build-publish-novo** — what has recently landed, what is
planned, and what is intentionally out of scope. It is a living document; nothing here is a
commitment to a date. For released changes see the [CHANGELOG](CHANGELOG.md); for upgrade steps see
the [migration guides](docs/migration/index.md).

## Where the suite stands

The suite already covers a broad Android release pipeline — Git-tag-based versioning, changelog
generation with pluggable rendering strategies, issue-title auto-resolution (Jira/ClickUp), and
distribution/notification across Firebase, Google Play, Slack, Telegram, Jira, Confluence, ClickUp,
and Nextcloud — with a strong emphasis on CI/CD friendliness (lazy configuration, no network at
configuration time, configuration-cache safety, variant-aware wiring). Compared with single-purpose
tools in this space, its **breadth** and **CI-friendliness** are the main differentiators:

- **git-changelog-gradle-plugin** goes deeper on a single changelog (Conventional Commits → semver,
  Handlebars templating) but does not distribute or notify.
- **Triple-T gradle-play-publisher** is the gold standard for Play Store publishing depth (listing
  metadata, screenshots, staged rollout, cross-track promotion) but is Play-only.
- **Fastlane** is broad but external (Ruby) rather than Gradle-native.

The roadmap below closes the gaps where those tools lead, without giving up the tag-first, CI-first
design.

## Recently shipped

- **Changelog issue auto-resolution** (2.1) — `CLOSES:` / `FIXES:` commit markers resolve to issue
  titles fetched from Jira/ClickUp via the provider-agnostic `IssueResolver` seam.
- **Jira multi-instance / multi-project registry** (2.1).

## Themes

### 1. Changelog engine depth

- **Conventional Commits + grouped sections** — classify commits by type (`feat`, `fix`, `perf`,
  `BREAKING CHANGE`, …) and render grouped sections. Additive to the existing marker-based flow;
  fits the strategy seam in `plugin-core`.
- **Semantic-version suggestion** — a task that reads Conventional-Commit types over the tag range
  and prints the implied bump, without displacing tags as the source of truth.
- **Template-based / multi-format output** — declarative Markdown / Keep-a-Changelog and JSON
  emitters alongside the current Kotlin strategy objects.

### 2. New integrations

New providers follow the established shape (a `shared/client-*` lib, a `plugin-build/plugin-*`, a
catalog alias, a docs page, a `plugin-test/*` module, and a `plugin-sender` block).

- **GitHub Releases publisher + GitHub Issues resolver** — create/update a GitHub Release for the
  current tag with the generated changelog; resolve `#123` markers to issue titles.
- **GitLab** — release publisher + issue resolver, same shape as GitHub.
- **Discord / Microsoft Teams / Mattermost notifications** — broaden messaging beyond Slack/Telegram.

### 3. Google Play depth

- **Refactor + test `TrackManager`** (carries the only refactor `TODO` in the codebase).
- **Cross-track promotion + staged-rollout bump** — match gradle-play-publisher's headline features.
- **Listing metadata & screenshots publishing.**

### 4. Quality & hardening

- ✅ **Fix the `IssueReference` substring match** — markers now match at a word boundary (`CLOSES`
  no longer matches inside `DISCLOSES`).
- ✅ **Add this `ROADMAP.md`.**
- ✅ **Rename the misspelled `core.enity` package → `core.entity`** (see the
  [2.1 migration guide](docs/migration/v2.1.md)).
- ✅ **Wire firebase/slack/nextcloud into `example-project`** so every advertised plugin has a live
  reference config.
- **Hermetic MockWebServer test tier** — make provider tests independent of live credentials and
  runnable on all CI OSes (`okhttpMockWebServer` is already in the version catalog).
- **Fill the weakest test spots** — Play, Firebase, and the `sender` aggregator.

## Out of scope

- The **legacy `build-publish` plugin** is deprecated and no longer developed. All work targets the
  `build-publish-novo` lineage.
- **Non-Gradle build systems** (Bazel, Maven) — the suite is intentionally Gradle-native.
