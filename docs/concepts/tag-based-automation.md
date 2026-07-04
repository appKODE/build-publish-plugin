[← Documentation](../../README.md)

# Tag-based automation (core concepts)

The core idea behind this plugin suite is **tag-based automation**.

Instead of storing version/build metadata in Gradle properties or CI variables, the plugin uses **Git tags** as the
single source of truth for:

- **Build number** (used as `versionCode` by default)
- **Build version** (used as `versionName` by default)
- **Changelog generation window** (diff between the last matching tag and HEAD)

### Why tags?

- **Deterministic and reproducible**
  - Tags are part of Git history and can be fetched in any environment (`git fetch --tags`).

- **Decoupled from branches and CI**
  - The same commit always has the same tag metadata, regardless of which CI system runs it.

- **Flexible automation foundation**
  - Multiple plugins (Jira/ClickUp/Slack/Telegram/Play/etc.) can rely on the same tag snapshot and changelog,
    which keeps automation consistent.

### How tag parsing works

The parser treats the **last numeric part before `-<variant>`** as the build number, and the preceding numeric parts
as the build version.

Examples:

```
v1.0.100-debug         -> buildVersion = 1.0,   buildNumber = 100
v1.2.3.42-release      -> buildVersion = 1.2.3, buildNumber = 42
app.2024.15-staging    -> buildVersion = 2024,  buildNumber = 15
```

### Default tag matching pattern

By default, the foundation plugin uses this regex template:

`DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"`

Where `%s` is replaced with the Android build variant name.

This means:

- Tags must include the build variant suffix (e.g. `-debug`, `-release`).
- Tags must contain at least one numeric group.

### Variant-specific tags (debug/release) and multiple tags per commit

Tag matching is **variant-aware**: each Android variant has its own tag stream because `%s` is replaced with the
variant name.

This enables a workflow where you can tag the **same commit** multiple times — once per build type/flavor:

```
v1.2.3.10-debug
v1.2.3.10-release
```

Both tags can point to the same commit SHA and are still treated as independent streams, because each variant uses
its own pattern and selection.

### How the plugin finds the “last tag” (`getLastTagSnapshot<Variant>`)

For each variant, the foundation plugin computes a regex from `buildTagPattern` (or `DEFAULT_TAG_PATTERN`) and then:

- lists all Git tags
- filters tags by the regex
- sorts tags primarily by commit order/time and then by extracted build number
- picks:
  - **`current`**: the first tag in the sorted list
  - **`previousInOrder`**: the second tag in the list (if present)
  - **`previousOnDifferentCommit`**: the first tag that points to a different commit (useful when multiple tags
    point to the same commit)

These values are stored in the tag snapshot JSON and reused by other tasks.

### Changelog range selection (why it uses the previous *commit* tag)

Changelog generation uses **`previousOnDifferentCommit`** (exposed as `snapshot.previous`) as the start of the commit
range, not `previousInOrder`.

Reason:

- It is valid to have multiple tags pointing to the **same commit for the same variant** (for example, you restart a
  CI build or re-run a release job and create a new tag without any new commits).
- In that situation, `previousInOrder` may point to a tag on the same commit, and using it as a range start would
  produce an empty/duplicate changelog.

By selecting the previous tag on a **different commit**, the changelog reflects the actual changes since the last
code change, while still allowing tag messages/metadata to be attached to the current build.

### Build number requirements (why `versionCode` must increase)

By default, `buildNumber` extracted from the tag is used as `versionCode`.
To keep versioning stable and monotonic:

- **Build numbers must be positive**
  - The plugin treats `0` and negative build numbers as invalid.

- **Build numbers must increase within the same variant tag stream**
  - The tag selection logic validates the last tags to ensure build numbers and commit chronology are consistent.
  - If the plugin detects that a “newer” tag has a build number that is not greater than the previous one,
    it fails with a detailed Gradle error.

This is one of the reasons tags are used as a core automation primitive: they provide a single, auditable,
monotonically-increasing sequence per variant.

### Customizing the tag pattern (`buildTagPattern`)

Configure `buildPublishFoundation.output.common.buildTagPattern { ... }` to match your tag naming convention.

Kotlin DSL (`build.gradle.kts`):

```kotlin
buildPublishFoundation {
    output {
        common {
            buildTagPattern {
                literal("v")
                separator(".")
                buildVersion()
                separator("-")
                buildVariantName()
            }
        }
    }
}
```

Groovy DSL (`build.gradle`):

```groovy
buildPublishFoundation {
    output {
        common {
            buildTagPattern { builder ->
                builder.literal('v')
                builder.separator('.')
                builder.buildVersion()
                builder.separator('-')
                builder.buildVariantName()
            }
        }
    }
}
```

### How automation consumes tags

- Foundation produces a tag snapshot via `getLastTagSnapshot<Variant>`.
- Other tasks/plugins read that snapshot to:
  - compute `versionName` / `versionCode`
  - generate a changelog (`generateChangelog<Variant>`)
  - attach version info to uploads / notifications

### Fallback behavior

If no matching tag is found, the foundation plugin can fall back to stub/default values.
This is controlled by `output.useStubsForTagAsFallback` and `output.useDefaultsForVersionsAsFallback`.

