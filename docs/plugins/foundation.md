[← Documentation](../../README.md) · [All plugins](./index.md)

# Foundation plugin

`ru.kode.android.build-publish-novo.foundation`

The core plugin that provides essential functionality for build publishing, version management, and changelog generation. This plugin must be applied to all modules that will use any of the publishing plugins.

This plugin supports only:

- Android **application** modules (`com.android.application`)
- Android Gradle Plugin **7.4+**

#### Key Features
- Automatic version management using Git tags
- Changelog generation from commit history
- Build variant support (flavors and build types)
- Customizable version code and name strategies
- Support for multiple output formats (APK, AAB)

#### Minimum Setup

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
   id("com.android.application")
   id("ru.kode.android.build-publish-novo.foundation")
}

buildPublishFoundation {
   output {
      common {
         baseFileName.set("app")
         useVersionsFromTag.set(true)

         // Matches tags like:
         // - v1.0.100-debug
         // - v1.2.3.42-release
         // The last numeric part is treated as the build number.
         buildTagPattern {
            literal("v")
            separator(".")
            buildVersion()
            optionalSeparator(".")
            anyOptionalSymbols()
            separator("-")
            buildVariantName()
         }
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
}

buildPublishFoundation {
    output {
        common {
            baseFileName.set('app')
            useVersionsFromTag.set(true)

            buildTagPattern {
                literal('v')
                separator('.')
                buildVersion()
                optionalSeparator('.')
                anyOptionalSymbols()
                separator('-')
                buildVariantName()
            }
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.KeyAndTitleResolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.ChangelogLineOrKeyUnresolvedStrategy

buildPublishFoundation {
   verboseLogging.set(false)
   bodyLogging.set(false)

   output {
      common {
         baseFileName.set("app")
         useVersionsFromTag.set(true)
         useDefaultsForVersionsAsFallback.set(true)

         versionNameStrategy {
            BuildVersionNumberVariantNameStrategy
         }

         versionCodeStrategy {
            BuildVersionCodeStrategy
         }

         outputApkNameStrategy {
            VersionedApkNamingStrategy
         }
      }

      buildVariant("debug") {
         useStubsForTagAsFallback.set(true)
      }
   }

   changelog {
      common {
         commitMessageKey.set("message")
         issueSources {
            issueSource("tracker") {
               numberPattern.set("#(\\d+)")
               urlPrefix.set("https://your-issue-tracker.com/issue/")
            }
         }
         // Resolve `CLOSES:` / `FIXES:` commit markers to issue titles via a provider plugin
         issueReferences {
            issueReference("closes") {
               key.set("CLOSES")
               numberPattern.set("(\\d+|[A-Z]+-\\d+)")
            }
            issueReference("fixes") {
               key.set("FIXES")
               numberPattern.set("(\\d+|[A-Z]+-\\d+)")
            }
         }
         resolvedIssueStrategy { KeyAndTitleResolvedStrategy }
         unresolvedIssueStrategy { ChangelogLineOrKeyUnresolvedStrategy }
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.KeyAndTitleResolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.ChangelogLineOrKeyUnresolvedStrategy

buildPublishFoundation {
    verboseLogging.set(false)
    bodyLogging.set(false)

    output {
        common {
            baseFileName.set('app')
            useVersionsFromTag.set(true)
            useDefaultsForVersionsAsFallback.set(true)

            versionNameStrategy {
              BuildVersionNumberVariantNameStrategy.INSTANCE
            }

            versionCodeStrategy {
                BuildVersionCodeStrategy.INSTANCE
            }

            outputApkNameStrategy {
                VersionedApkNamingStrategy.INSTANCE
            }
        }

        buildVariant('debug') {
            useStubsForTagAsFallback.set(true)
        }
    }

    changelog {
        common {
            commitMessageKey.set('message')
            issueSources {
                issueSource('tracker') {
                    numberPattern.set('#(\\d+)')
                    urlPrefix.set('https://your-issue-tracker.com/issue/')
                }
            }
            // Resolve `CLOSES:` / `FIXES:` commit markers to issue titles via a provider plugin
            issueReferences {
                issueReference('closes') {
                    key.set('CLOSES')
                    numberPattern.set('(\\d+|[A-Z]+-\\d+)')
                }
                issueReference('fixes') {
                    key.set('FIXES')
                    numberPattern.set('(\\d+|[A-Z]+-\\d+)')
                }
            }
            resolvedIssueStrategy { KeyAndTitleResolvedStrategy.INSTANCE }
            unresolvedIssueStrategy { ChangelogLineOrKeyUnresolvedStrategy.INSTANCE }
        }
    }
}
```

#### Configuration Reference

##### Root properties (`buildPublishFoundation { ... }`)

- **`verboseLogging`**
  - **What it does**: Enables extra informational logging from build-publish plugins.
  - **Why you need it**: Useful for debugging why a particular config (for example `common` vs `buildVariant(...)`) was chosen and what tasks were configured.
  - **When to enable**: CI troubleshooting, local debugging.

- **`bodyLogging`**
  - **What it does**: Enables logging of HTTP request/response bodies for plugins that talk to external APIs.
  - **Why you need it**: Helps troubleshoot API failures or unexpected responses.
  - **Warning**: Can print sensitive data. Prefer keeping it disabled in CI.

##### Output (`buildPublishFoundation { output { ... } }`)

Output configuration is defined per Android build variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("debug") { ... }` to override for a single variant

Properties (applies to each `OutputConfig`):

- **`baseFileName`** *(required)*
  - **What it does**: Base name used by output file naming strategy.
  - **Why you need it**: Ensures produced APKs are easy to recognize (for example `app-release-...apk`).

- **`useVersionsFromTag`** *(default: `true`)*
  - **What it does**: Reads version info from Git tags.
  - **Why you need it**: Single source of truth for `versionName`/`versionCode` across builds.
  - **If disabled**: Version values are taken from fallbacks (defaults or Android DSL depending on other settings).

- **`useStubsForTagAsFallback`** *(default: `true`)*
  - **What it does**: If no matching Git tag is found, allows the build to continue using stub tag values.
  - **Why you need it**: Useful for first CI runs / new branches where tags aren’t present yet.
  - **Implementation detail**: Stub tag values are generated via `ru.kode.android.build.publish.plugin.core.strategy.HardcodedTagGenerationStrategy`.
  - **If disabled**: Missing tags typically cause the tag snapshot task to fail.

- **`useDefaultsForVersionsAsFallback`** *(default: `true`)*
  - **What it does**: Uses default version values when tag-derived values are unavailable.
  - **Why you need it**: Lets builds proceed even when tag parsing is temporarily unavailable.

- **`buildTagPattern { ... }`**
  - **What it does**: Builds a regex template that is used to find the “latest” tag for each variant.
  - **Why you need it**: Your repository’s tag format must match what the plugin expects.
  - **Important**: The pattern must contain:
    - at least one `buildVersion()` group (`(\\d+)`)
    - `buildVariantName()` (`%s`) so each variant can have its own tag stream

- **`versionNameStrategy { ... }`**
  - **What it does**: Defines how `versionName` is computed from a resolved tag.
  - **Why you need it**: Different projects encode different info into tags.
  - **Groovy DSL note**: Kotlin `object` strategies are referenced via `INSTANCE` (no `new`), for example
    `versionNameStrategy { BuildVersionNumberNameStrategy.INSTANCE }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionVariantNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.TagRawNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.FixedVersionNameStrategy { ... }`
  - **Examples** (assume `buildVariant.name = "release"`, tag present: `tag.buildVersion = "1.2"`, `tag.buildNumber = 42`, `tag.name = "v1.2.42-release"`; tag missing: `tag = null`):
    - `BuildVersionNameStrategy`
      - tag present: `1.2`
      - tag missing: `0.0`
    - `BuildVersionNumberNameStrategy`
      - tag present: `1.2.42`
      - tag missing: `0.0.1`
    - `BuildVersionNumberVariantNameStrategy`
      - tag present: `1.2.42-release`
      - tag missing: `0.0-release`
    - `BuildVersionVariantNameStrategy`
      - tag present: `1.2-release`
      - tag missing: `0.0-release`
    - `TagRawNameStrategy`
      - tag present: `v1.2.42-release`
      - tag missing: `v0.0.1-release`
    - `FixedVersionNameStrategy { "my-fixed" }`
      - tag present: `my-fixed`
      - tag missing: `my-fixed`

- **`versionCodeStrategy { ... }`**
  - **What it does**: Defines how `versionCode` is computed from a resolved tag.
  - **Why you need it**: Allows you to encode semantic versioning or fixed version code rules.
  - **Groovy DSL note**: Kotlin `object` strategies are referenced via `INSTANCE` (no `new`), for example
    `versionCodeStrategy { BuildVersionCodeStrategy.INSTANCE }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.FixedVersionCodeStrategy { ... }`
  - **Examples** (assume tag present: `tag.buildVersion = "1.2"`, `tag.buildNumber = 42`; tag missing: `tag = null`):
    - `BuildVersionCodeStrategy`
      - tag present: `42`
      - tag missing: `1`
    - `SemanticVersionFlattenedCodeStrategy` (formula: `(major * 1000 + minor) * 1000 + buildNumber`)
      - tag present (`1.2` + `42`): `1002042`
      - tag missing: `1`
    - `FixedVersionCodeStrategy { 10000 }`
      - tag present: `10000`
      - tag missing: `10000`

- **`outputApkNameStrategy { ... }`**
  - **What it does**: Defines how the final APK file name is computed.
  - **Why you need it**: Produces consistent artifact names for distribution/upload steps.
  - **Groovy DSL note**: Kotlin `object` strategies are referenced via `INSTANCE` (no `new`), for example
    `outputApkNameStrategy { VersionedApkNamingStrategy.INSTANCE }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.SimpleApkNamingStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.FixedApkNamingStrategy { ... }`
  - **Examples** (assume `baseFileName = "app"`, `outputFileName = "app-release.apk"`, and tag present: `tag.buildVariant = "release"`, `tag.buildNumber = 42`; tag missing: `tag = null`):
    - `VersionedApkNamingStrategy`
      - tag present: `app-release-vc42-<DATE>.apk` (date format: `ddMMyyyy`)
      - tag missing: `app-<DATE>.apk`
    - `SimpleApkNamingStrategy`
      - tag present: `app.apk`
      - tag missing: `app.apk`
    - `FixedApkNamingStrategy { "my-fixed" }`
      - tag present: `my-fixed.apk`
      - tag missing: `my-fixed.apk`

##### Changelog (`buildPublishFoundation { changelog { ... } }`)

Changelog config defines how commit messages are filtered and how issue links are rendered.

- **`issueSources { ... }`**
  - **What it does**: A named container of issue-tracker sources. Each named source pairs a
    `numberPattern` (regex that finds issue keys in commit messages) with an optional `urlPrefix`
    (the base URL those keys link to). Issue extraction (Jira/ClickUp automation) unions every
    source's `numberPattern`; link rendering (Slack/Telegram) resolves each matched key against
    **its own** source's `urlPrefix`.
  - **Why you need it**: A single changelog can reference issues that live in different projects
    and/or on different issue-tracker hosts. Each source links only its own matches, so keys from
    two Jira instances get correct per-host links natively.
  - **Per-source properties**:
    - **`numberPattern`** *(required)*: Java regex matching issue keys (for example `"BASE-\\d+"`).
    - **`urlPrefix`** *(optional)*: Base URL the matched key is appended to (for example
      `"https://jira1/browse/"`). Omit/leave blank to use the source for extraction only — its keys
      are not turned into links.
  - **Example** (two projects on two hosts):
    ```kotlin
    issueSources {
      issueSource("base") {
        numberPattern.set("BASE-\\d+")
        urlPrefix.set("https://jira1/browse/")
      }
      issueSource("leg") {
        numberPattern.set("LEG-\\d+")
        urlPrefix.set("https://jira2/browse/")
      }
    }
    ```
  - **Single-source shorthand**: when there is only one source you can skip the `issueSources { }`
    wrapper and call `issueSource("name") { … }` — or the unnamed `issueSource { … }` — directly
    inside `changelog { common { … } }`.
  - **Note**: Source patterns are assumed disjoint (each issue key matches exactly one source).
    Overlapping patterns would link a key more than once.

- **`issueReferences { ... }`**
  - **What it does**: A named container of commit-message *references*. Each named reference pairs a
    `key` (the commit marker, for example `CLOSES`) with a `numberPattern` (regex that extracts the
    issue token appearing after the key on that line — a bare number like `3458` or a prefixed key
    like `TBI-3458`). Whereas `issueSources` only turns issue keys into hyperlinks, `issueReferences`
    asks a provider plugin (Jira and/or ClickUp) to resolve each referenced token to its
    human-readable issue **title** and writes that into the changelog. The two blocks coexist, as do
    manual `CHANGELOG:` lines.
  - **Why you need it**: Developers write a single short marker in the commit body — for example
    `CLOSES: 3458` or `FIXES: TBI-3459` — and the changelog is enriched with the actual issue titles
    (`• [TBI-3458] Fix cold start`) instead of a bare key or a hand-typed summary.
  - **Per-reference properties**:
    - **`key`** *(required)*: The commit marker that starts the line (for example `"CLOSES"`).
    - **`numberPattern`** *(required)*: Java regex matching the issue token after the key. Use
      `"(\\d+|[A-Z]+-\\d+)"` to accept both bare numbers and prefixed keys.
  - **Example** (two markers):
    ```kotlin
    issueReferences {
      issueReference("closes") {
        key.set("CLOSES")
        numberPattern.set("(\\d+|[A-Z]+-\\d+)")
      }
      issueReference("fixes") {
        key.set("FIXES")
        numberPattern.set("(\\d+|[A-Z]+-\\d+)")
      }
    }
    ```
    ```groovy
    issueReferences {
      issueReference('closes') {
        key.set('CLOSES')
        numberPattern.set('(\\d+|[A-Z]+-\\d+)')
      }
      issueReference('fixes') {
        key.set('FIXES')
        numberPattern.set('(\\d+|[A-Z]+-\\d+)')
      }
    }
    ```
  - **Single-reference shorthand**: when there is only one reference you can skip the
    `issueReferences { }` wrapper and call `issueReference("name") { … }` directly inside
    `changelog { common { … } }` (mirrors the `issueSource` shorthand).
  - **Commit convention**: write one marker line per issue in the commit body, with no manual title:
    `CLOSES: 3458` or `FIXES: TBI-3459`. A manual `CHANGELOG: …` line still works and coexists; if a
    commit carries both a `CHANGELOG:` line and a `CLOSES:`/`FIXES:` for the same token, only one
    entry is emitted (deduplicated).
  - **Requires a provider**: resolution happens only when a provider plugin (Jira and/or ClickUp) has
    its `issueResolution { }` block enabled (see the [Jira](./jira.md) and [ClickUp](./clickup.md)
    docs). With no provider/resolver registered, the reference pass is skipped and the changelog is
    left unchanged.

- **`resolvedIssueStrategy { ... }`**
  - **What it does**: Controls how a *resolved* reference (title successfully fetched) is rendered.
  - **Import from**: `ru.kode.android.build.publish.plugin.core.strategy`.
  - **Built-in variants**:
    - `KeyAndTitleResolvedStrategy` *(default)* → `• [TBI-3458] Fix cold start`
    - `TitleOnlyResolvedStrategy` → `• Fix cold start`
    - `KeyOnlyResolvedStrategy` → `• [TBI-3458]`
  - **Usage**: `resolvedIssueStrategy { TitleOnlyResolvedStrategy }`

- **`unresolvedIssueStrategy { ... }`**
  - **What it does**: Controls how an *unresolved* reference is rendered (unknown issue, offline,
    missing provider). Resolution is always **non-blocking** — an unresolved reference never fails
    the build.
  - **Import from**: `ru.kode.android.build.publish.plugin.core.strategy`.
  - **Built-in variants**:
    - `ChangelogLineOrKeyUnresolvedStrategy` *(default)* → uses the commit's own `CHANGELOG:` line if
      present, otherwise the bare `• [TBI-3458]`
    - `KeyOnlyUnresolvedStrategy` → always `• [TBI-3458]`
    - `SkipUnresolvedStrategy` → omits the entry entirely
    - `FallbackTextUnresolvedStrategy("(no description)")` → `• [TBI-3458] (no description)`
  - **Usage**: `unresolvedIssueStrategy { KeyOnlyUnresolvedStrategy }`

- **`commitMessageKey`**
  - **What it does**: Marker used to include only selected commits into changelog.
  - **Why you need it**: Keeps changelog clean (only user-facing changes).

- **`annotatedTagMessageStrategy { ... }`**
  - **What it does**: Defines how Git *annotated tag* messages are formatted when included into the changelog.
  - **Default**: `ru.kode.android.build.publish.plugin.core.strategy.DecoratedAnnotatedTagMessageStrategy`

- **`changelogMessageStrategy { ... }`**
  - **What it does**: Defines how each commit message is converted into a changelog line.
  - **Default**: `ru.kode.android.build.publish.plugin.core.strategy.KeyRemovingChangelogMessageStrategy`
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.KeyRemovingChangelogMessageStrategy` (hides the key)
    - `ru.kode.android.build.publish.plugin.core.strategy.KeyPreservingChangelogMessageStrategy` (keeps the key)

- **`emptyChangelogMessageStrategy { ... }`**
  - **What it does**: Defines what gets written to the changelog file when there are no matching commits.
  - **Default**: `ru.kode.android.build.publish.plugin.core.strategy.NoChangesChangelogMessageStrategy`

- **`notGeneratedChangelogMessageStrategy { ... }`**
  - **What it does**: Defines what gets logged when changelog generation produced an empty result.
  - **Default**: `ru.kode.android.build.publish.plugin.core.strategy.NoChangesNotGeneratedChangelogMessageStrategy`

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `getLastTagSnapshot<Variant>` | Finds the last matching Git tag and writes a JSON snapshot file | - |
| `computeVersionCode<Variant>` | Computes `versionCode` (from tag or fallback) and writes it to a file | `getLastTagSnapshot<Variant>` |
| `computeVersionName<Variant>` | Computes `versionName` (from tag or fallback) and writes it to a file | `getLastTagSnapshot<Variant>` |
| `computeApkOutputFileName<Variant>` | Computes the final APK output file name and writes it to a file | `getLastTagSnapshot<Variant>` |
| `renameApk<Variant>` | AGP artifact transform: copies/renames the produced APK to the computed output name | `computeApkOutputFileName<Variant>` |
| `printLastIncreasedTag<Variant>` | Prints the next tag name (build number increment) based on the snapshot | `getLastTagSnapshot<Variant>` |
| `generateChangelog<Variant>` | Generates a changelog between last tag and HEAD | `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Get last tag for debug variant
./gradlew getLastTagSnapshotDebug

# Generate changelog for release variant
./gradlew generateChangelogRelease

# Print last increased tag for staging variant
./gradlew printLastIncreasedTagStaging
```

---

