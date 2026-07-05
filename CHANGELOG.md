# Changelog

## 🚨 Important: Project lineage change

Starting with **build-publish-novo**, this project introduces a **new package name**, **new internal architecture**, and **independent versioning**.

- `build-publish-novo` is **not backward-compatible** with the legacy `build-publish` plugin
- Version numbers **do not continue** the legacy sequence
- Legacy versions are preserved below **for reference only**

---

## 🚀 build-publish-novo (new lineage)

### 2.1.0

> **Breaking release** for Jira automation project config. Upgrading from 2.0? Follow the
> **[migration guide to 2.1](docs/migration/v2.1.md)**. Use with **build-publish-novo-core 2.1.0**.

* **Auto-resolve changelog titles:** a commit `CLOSES: <num>` / `FIXES: <num>` line is now resolved to the issue's title (fetched from Jira/ClickUp) and inserted into the changelog — no more hand-copied `CHANGELOG: [KEY] <title>` line. Manual `CHANGELOG:` entries still work and coexist (matching forms are de-duplicated)
* Foundation `changelog { }` gains `issueReferences { issueReference("name") { key } }` (unwrapped `issueReference` shorthand for one marker) declaring which commit markers to auto-resolve, alongside the existing `issueSources { }` (which only links keys). `numberPattern` is optional — it defaults to the standard bare-number-or-prefixed-key regex (`(\d+|[A-Z]+-\d+)`), so usually only `key` is set
* Foundation `changelog { }` gains `resolvedIssueStrategy { }` (`KeyAndTitleResolvedStrategy` default / `TitleOnlyResolvedStrategy` / `KeyOnlyResolvedStrategy`) and `unresolvedIssueStrategy { }` (`ChangelogLineOrKeyUnresolvedStrategy` default / `KeyOnlyUnresolvedStrategy` / `SkipUnresolvedStrategy` / `FallbackTextUnresolvedStrategy`). Resolution is non-blocking — an unresolved reference never fails the build
* Provider-agnostic `IssueResolver` seam in `plugin-core`: any provider plugin contributes a resolver, so Jira and ClickUp (and future sources) can enrich the same shared changelog
* **Breaking:** Jira projects are now declared once in a shared registry nested under each `auth` instance (`instance("x") { project("app") { projectKey } }`); `projectKey` must be globally unique and no longer lives on automation projects. `automation` selects projects two-level via `targetInstance("name") { projectNames(…) / project("x") { …overrides } }` (replacing self-contained `projects { project { projectKey; instanceName } }`)
* Jira `issueResolution { common { fromInstance("name") { projectNames(…) } } }` opt-in block resolves `CLOSES`/`FIXES` references to Jira issue titles; bare numbers resolve against the sole selected project, prefixed keys route by prefix. Declaring the block is the opt-in — there is no separate `enabled` flag
* ClickUp `issueResolution { }` opt-in block resolves ClickUp task ids to task names
* Clients: `client-jira` adds `getIssueSummary`, `client-clickup` adds `getTaskName`

### 2.0.0

> **Breaking release.** Upgrading from 1.x? Follow the
> **[migration guide to 2.0](docs/migration/v2.md)**. Use with **build-publish-novo-core 2.0.0**.

* Update deps: Kotlin `2.4.0`, KSP `2.3.9`, AGP `9.2.1` (min Gradle `9.4.1`), Gradle wrapper `9.6.1`, Firebase App Distribution `5.3.0`, OkHttp `5.4.0`, JUnit BOM `6.1.1`, Google Auth `1.48.0`, kotlinx.serialization `1.11.0`, Vanniktech Maven Publish `0.37.0`, Play Publisher `v3-rev20260625`; add an AGP `9.2.1` assemble test
* **Breaking:** changelog `issueNumberPattern` / `issueUrlPrefix` replaced by named `issueSources { issueSource("name") { numberPattern; urlPrefix } }` (unwrapped `issueSource` shorthand for one source); standalone `--issueNumberPattern` → repeatable `--issuePattern`
* **Breaking:** Jira `auth { }` now holds named `instance("name") { baseUrl; credentials }` (like Telegram bots) instead of one variant-keyed credential set; projects select one via `instanceName`, defaulting to `default`
* **Breaking:** Jira automation targets self-contained `projects { project("name") { projectKey; instanceName?; labelPattern?/fixVersionPattern?/targetStatusName? } }` routed by issue-key prefix (unwrapped `project` shorthand for one); automation-level `projectKey`/patterns and their `--projectKey`/`--*Pattern` overrides removed; unknown `instanceName` / duplicate keys fail fast; standalone Jira tasks gain `--instanceName`
* Expose `build-publish-novo-core` as an `api` dependency so its public DSL types (strategies, `CollectionStrategy`, config bases) reach consumers transitively; `client-*` stay `implementation`
* Add Groovy `Closure` overloads (with `@DelegatesTo`) across the nested-container DSL methods
* Extract network clients into publishable `build-publish-novo-client-{slack,telegram,nextcloud,jira,confluence,clickup}` libraries, consumed via the catalog / composite-build substitution
* Add standalone CLI tasks per integration plus an Android-free `plugin-sender` aggregator; shared multi-step logic lives in controller action functions
* Jira: a project with no matching changelog issues now logs an info message and is skipped; `transitionJiraIssue` uppercases the project key and fails fast on a missing transition
* Confluence client: fix silently-swallowed attachment/comment removal failures; add formatted write errors

### 1.2.17
* Add Nextcloud plugin (`build-publish-novo-nextcloud`) for uploading builds and changelogs to Nextcloud
* Fix CI/CD race condition when multiple tags exist on the same commit: use `CI_COMMIT_TAG` env var for exact tag resolution
* Should be used with **1.2.17 version of build-publish-novo-core**

### 1.2.16
* Fix logic to send changelog with empty mentions
* Should be used with **1.2.8 version of build-publish-novo-core**

### 1.2.15
* Fix `SemanticVersionFlattenedCodeStrategy` crash when tag contains 3-part version (e.g. `v4.0.0.100-release`)
* Should be used with **1.2.8 version of build-publish-novo-core**

### 1.2.14
* Increase Telegram client timeout to 6m

### 1.2.13
* Update deps, add Bundle renaming and corresponding strategy to configure it
* Should be used with **1.2.7 version of build-publish-novo-core**

### 1.2.12
* Fix message for cannotReturnTagMessage to show correct last commit date
* Should be used with **1.2.6 version of build-publish-novo-core**

### 1.2.11
*Fix logic to identify that datetime of tag is in the future

### 1.2.10
* Apply proxy settings to Firebase App Distribution task

### 1.2.9
* Print info about not found Jira transition id only once

### 1.2.8
* Not fail build if required Jira transition id not found, just log warning

### 1.2.7
* Add Closure methods for all configurations
* Should be used with **1.2.4 version of build-publish-novo-core**

### 1.2.6
- Split strategies, remove excludeMessageKey property, now it is configured via strategy

### 1.2.5
- Add strategies to configure changelog message/annotated tag message/empty messages
- Replace empty changelogs message to more compact versions
- Add logic to map Markdown tags to HTML for Telegram to style correctly it

### 1.2.4
- Fix issue with publishing Telegram changelog with html tags
- Update README for Slack to add info how install token correctly for file uploading
- Update README for buildTagPattern Gradle example 
- Add logic to show show description and message in RequestError

### 1.2.3
- Add support of agp 9.0.0

### 1.2.2
- Decouple logic from specific gradle version implementation 
- Replace ServiceReference with Internal to support older Gradle versions
- Increase min required Gradle version from 7.0.4 to 7.4.0
- Add usesService for tasks where it was missed 
- Move AGP version tests to the separate module and fix logic to resolve and specify required versions 

### 1.2.1
- Add Telegram and Confluence distribution compression option, by default it is disabled

### 1.2.0
**Breaking changes**
- Introduce modular `build-publish-novo-*` plugin suite (apply only required integrations)
- Extract shared logic into `ru.kode.android:build-publish-novo-core`
- Remove AppCenter integration (delete related config/tasks and migrate to another distribution channel)
- Rename `getLastTag<Variant>` → `getLastTagSnapshot<Variant>`
- Default `versionName` is now derived from tag build version only (`1.2` / `1.2.3`);
  to include build number, configure `BuildVersionNumberNameStrategy`

**Features**
- Variant-aware tag snapshot with validation (positive build numbers, monotonic per variant stream)
- Changelog generation based on previous tag on a different commit (supports CI re-tags without empty changelogs)
- Telegram lookup task to discover `chatId` / `topicId`
- Support custom Telegram Bot API server

**Internal**
- Expanded plugin-test coverage
- Improved CI secret handling patterns (including base64-decoded secret files)

---

## 📦 build-publish-novo-core

> Covers the `build-publish-novo-core` library and the sibling `build-publish-novo-client-*`
> libraries, which are versioned and published together from the `shared` build.

### 2.1.0
* **Breaking (custom plugins only):** fix a typo in the core package name — the domain entity types (`Tag`, `BuildVariant`, `BuildTagSnapshot`, `CommitRange`, `ExtensionInput`, `IssueReference`, `IssueSource`) move from `core.enity` to `core.entity`. Consumers using only the published plugins are unaffected; custom-plugin authors replace `core.enity` imports with `core.entity` (see the [2.1 migration guide](docs/migration/v2.1.md))
* Add a provider-agnostic issue-resolution seam: the `IssueResolver` interface + `ResolvedIssue` (`core/issue`) and the `IssueReference` entity (`core/entity`), so any plugin can resolve changelog `CLOSES`/`FIXES` markers to issue titles
* Add serializable, configuration-cache-safe rendering strategies: `ResolvedIssueStrategy` (`KeyAndTitleResolvedStrategy` default / `TitleOnlyResolvedStrategy` / `KeyOnlyResolvedStrategy`) and `UnresolvedIssueStrategy` (`ChangelogLineOrKeyUnresolvedStrategy` default / `KeyOnlyUnresolvedStrategy` / `SkipUnresolvedStrategy` / `FallbackTextUnresolvedStrategy`)
* `GitCommandExecutor.extractIssueReferenceLines` / `GitRepository.issueReferenceLines` extract `CLOSES`/`FIXES` lines (paired with each commit's `CHANGELOG:` line via `CommitIssueReferences`); `GitChangelogBuilder.buildForSnapshot` resolves them in order, de-duplicates against manual entries and duplicate keys, and never fails the build on an unresolved reference (a warning is logged)
* `GenerateChangelogTaskOutput` exposes a `ListProperty<IssueResolver>` injection point so provider plugins contribute resolvers without coupling the core to any tracker
* Clients: `build-publish-novo-client-jira` adds `getIssueSummary` (backed by a new `GET issue/{key}?fields=summary` call + `GetIssueSummaryResponse`); `build-publish-novo-client-clickup` adds `getTaskName` (reusing the existing `GET task/{id}` call)

### 2.0.0
* Bump build/runtime deps: Kotlin `2.4.0`, KSP `2.3.9`, AGP `9.2.1` (min Gradle `9.4.1`), OkHttp `5.4.0`, Retrofit `3.0.0`, kotlinx.serialization `1.11.0`, JUnit BOM `6.1.1`
* **Breaking:** replace the loose changelog `issueNumberPattern` / `issueUrlPrefix` inputs with the named `IssueSource` entity, enabling multiple issue sources (each with its own `numberPattern` / `urlPrefix`) in one changelog
* Expose the public DSL surface (strategies, `CollectionStrategy`, config bases such as `BasicAuthCredentials`) as a stable `api` dependency, so plugins re-export it to consumers transitively; `client-*` stay `implementation`
* Add Groovy `Closure` overloads (`@DelegatesTo`) across the shared nested-container config bases
* Extract the network clients into publishable `build-publish-novo-client-{slack,telegram,nextcloud,jira,confluence,clickup}` libraries built on the core, consumed via the version catalog / composite-build substitution

### 1.2.17
* Add `ciCommitTag` support in `GitCommandExecutor` and `GitRepository` to fix tag resolution race condition on concurrent CI pipelines

### 1.2.16
* Version bump to align with main plugin versioning (no functional changes from 1.2.8)

### 1.2.8
- Fix `SemanticVersionFlattenedCodeStrategy` to correctly parse 3-part versions (MAJOR.MINOR.PATCH)

### 1.2.7
- Update deps (Kotlin -> 2.3.10, AGP -> 9.0.1, KSP -> 2.3.5, Firebase -> 5.2.1, Google Auth -> 1.43.0)
- Add logic to handle Bundle renaming

### 1.2.6
- Fix message for cannotReturnTagMessage to show correct last commit date

### 1.2.5
- Add logging for proxy authentication
- Decrease date/time precision to minutes for tag snapshot

### 1.2.4
* Add Closure methods for extension configuration

### 1.2.3
- Add strategies to configure changelog message/annotated tag message/empty messages

### 1.2.1
- Decouple logic from specific gradle version implementation

### 1.2.0
- Shared core library used by all `novo` plugins
- Tag snapshot model and tag-range utilities for changelog generation
- Version strategies (`VersionNameStrategy`, `VersionCodeStrategy`)
- Tag pattern builder utilities

---

## 🗃 Legacy build-publish (deprecated)

> ⚠️ The versions below belong to the **legacy package name and versioning scheme**.  
> This plugin is deprecated and no longer developed.

### 1.3.0-alpha18
- Fix logic to set Telegram distribution properties

### 1.3.0-alpha17
- Fix logic to provide correct Telegram auth credentials

### 1.3.0-alpha16
- Add custom host for Telegram bot with basic auth
- Add task to publish bundle for Telegram distribution

### 1.3.0-alpha15
- Ignore proxy settings according to `nonProxyHosts`

### 1.3.0-alpha14
- Configure OkHttp proxy settings for authorization
- Disable caching by default for tag-related tasks

### 1.3.0-alpha13
- Update Slack upload logic to new Slack API flow

### 1.3.0-alpha12
- Revert logic for `versionCode` and `versionName` providers

### 1.3.0-alpha11
- Add ability to disable default configurations

### 1.3.0-alpha10
- Disable fallback for `versionCode` and `versionName`

### 1.3.0-alpha09
- Upload build as Confluence comment

### 1.3.0-alpha08
- Send Telegram changelog in chunks
- Fix Telegram entity escaping issues

### 1.3.0-alpha07
- Add max character limit for AppCenter notes
- Split large changelog attachments

### 1.3.0-alpha06
- Upload builds to Telegram via `telegramDistributionUpload*` tasks

### 1.3.0-alpha05
- Automate ClickUp workflows via `clickUpAutomation` tasks

### 1.3.0-alpha04
- Fix omitted Google Play track edit commit

### 1.3.0-alpha03
- Implement Google Play upload via `playUpload*` tasks

### 1.3.0-alpha02
- Update AGP to 8.3.1 and Gradle to 8.7

### 1.3.0-alpha01
- Experimental configurable build tag pattern (single `-` delimiter restriction)

### 1.2.0
- Internalize Firebase App Distribution plugin
- Add `useVersionsFromTag` option
- Restructure plugin (build conventions, version catalog)
- Update Kotlin to 1.9.22 and dependencies

### 1.1.0-alpha21
- Hide Telegram webhook URL inside plugin
- Refactor Slack attachment handling

### 1.1.0-alpha20
- Optional Telegram topic ID support

### 1.1.0-alpha19
- Ellipsize Slack changelog
- Promote debug/warn logs to info

### 1.1.0-alpha18
- Sort tags by build number to detect last tag correctly

### 1.1.0-alpha17
- Fix AppCenter changelog descriptions
- Update Gradle and Kotlin versions
- Fix Telegram hyperlink attachments

### 1.1.0-alpha16
- Replace shell scripts with Retrofit + OkHttp

### 1.1.0-alpha15
- Migrate git commands to `grgit`
- Improve AppCenter app name configuration
- Refactor AppCenter upload status checks

### 1.1.0-alpha14
- AppCenter distribution task
- Jira automation (status, labels, fix version)
- Refactor configuration into domain-specific blocks
- Optimize tag-based versioning and changelog generation

### 1.0.5-hotfix01 – 2022-06-01
- Revert fix from 1.0.5

### 1.0.5 – 2022-05-30
- Fix last-tag detection logic

### 1.0.4 – 2022-05-26
- Add dimensions support for complex task generation

### 1.0.3 – 2022-05-20
- Override App ID for Firebase App Distribution

### 1.0.2 – 2022-05-16
- Add `PrintLastIncreasedTag` task

### 1.0.1 – 2022-05-06
- Configure distribution artifact type (APK / AAB)
