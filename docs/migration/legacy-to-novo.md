[← Documentation](../../README.md)

# Migrating from legacy `build-publish` to `build-publish-novo`

> This guide covers the one-time move from the **old, non-`novo`** plugin to the `build-publish-novo`
> lineage. Already on a `novo` 1.x release? See the [2.0 migration guide](./v2.md) instead.

If you are migrating from an older/legacy version of this plugin suite to the `*-novo` line, treat it as a
**breaking change** and do a quick audit of plugin IDs, dependencies, and your tag/versioning setup.

High-level changes introduced in the `novo` line:

- The plugin is now **modular**: each integration is a separate Gradle plugin (`foundation`, `slack`, `telegram`, `jira`, `confluence`, `nextcloud`, `clickup`, `play`, `firebase`).
- Common logic is extracted into a shared core library (`ru.kode.android:build-publish-novo-core`).
- Tag-based automation is **variant-aware** by default and validates tag ordering and build numbers.
- **AppCenter integration was removed** (if you used it previously, delete related configuration/tasks and replace with another distribution channel).

### 1) Update plugin IDs

Update all plugin IDs to the `ru.kode.android.build-publish-novo.*` namespace.

Recommendation:

- Search your build logic for `build-publish` and update IDs/artifacts accordingly.

### 2) Update dependency coordinates (Version Catalog / build-logic)

If you apply Build Publish plugins from a convention module (`build-logic` / `build-conventions`), make sure you use
the **novo** artifacts, for example:

- `ru.kode.android:build-publish-novo-core:...`
- `ru.kode.android.build-publish-novo.<plugin>:ru.kode.android.build-publish-novo.<plugin>.gradle.plugin:...`

Also note that there is no single “all-in-one” plugin anymore: if your old setup had one plugin that configured
multiple integrations, you now add/apply the exact set of `novo` plugins you need.

### 2.1) Update extension blocks (DSL)

The configuration is split into per-plugin extensions.

- Old setup (legacy): typically one root extension or a combined configuration block.
- New setup (novo): configure each integration via its own extension:
  - `buildPublishFoundation { ... }`
  - `buildPublishSlack { ... }`
  - `buildPublishTelegram { ... }`
  - `buildPublishJira { ... }`
  - `buildPublishConfluence { ... }`
  - `buildPublishNextcloud { ... }`
  - `buildPublishClickUp { ... }`
  - `buildPublishPlay { ... }`
  - `buildPublishFirebase { ... }`

### 3) Ensure the foundation plugin is applied

All other plugins rely on the foundation plugin to run variant configuration.
Make sure it is applied in every Android **application** module that uses any publishing/integration plugin:

- `ru.kode.android.build-publish-novo.foundation`

### 4) Verify tag-based versioning still matches your repository

The `novo` line is strongly built around tag-based automation.
Before running CI, verify that tags exist and match your variant(s):

```bash
./gradlew getLastTagSnapshotRelease
./gradlew printLastIncreasedTagRelease
```

Breaking change note: in the `novo` line the legacy `getLastTag<Variant>` task is renamed to
`getLastTagSnapshot<Variant>`.

Important behavior changes to account for:

- **Build numbers must be positive** (`0` and negative values are treated as invalid).
- **Build numbers must increase within the same variant tag stream**.
- Tag selection is variant-aware (tags typically end with `-debug`, `-release`, etc.).

What the foundation plugin does per variant:

- resolves a tag regex from `buildTagPattern` (or the default)
- picks the latest matching tag and writes a JSON snapshot file
- downstream tasks use that snapshot for `versionCode` / `versionName` / changelog / uploads

If your previous setup used a different tag naming scheme, configure `buildPublishFoundation.output.common.buildTagPattern { ... }`.

### 4.1) Breaking change: `versionName` strategy

In the `novo` line, the default `versionName` is derived from the parsed tag **build version** only
(for example `1.2` or `1.2.3`).

If in the legacy version you relied on `versionName` including the build number from the tag, configure a different
strategy explicitly, for example `BuildVersionNumberNameStrategy`.

Kotlin DSL (`build.gradle.kts`):

```kotlin
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy

buildPublishFoundation {
    output {
        buildVariant("internal") {
            baseFileName = "android"
            versionNameStrategy { BuildVersionNumberNameStrategy }
        }
    }
}
```

Groovy DSL (`build.gradle`): Kotlin `object` strategies are referenced via `INSTANCE` (no `new`), while class strategies must be instantiated (use `new ...()`):

```groovy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy

buildPublishFoundation {
  output {
    buildVariant('internal') {
      baseFileName = 'android'
      versionNameStrategy { BuildVersionNumberNameStrategy.INSTANCE }
    }
  }
}
```

If you configure this from a convention module (`build-logic` / `build-conventions`), make sure the module has access
to the core classes by adding `ru.kode.android:build-publish-novo-core` to its dependencies (see Installation section).

### 5) Re-check secrets handling

When migrating, re-check all credentials configuration:

- Prefer CI/CD secret variables or `local.properties` for local development.
- For string secrets, use `providers.environmentVariable("...")`.
- For some file-based secrets used by Worker API / shared services, resolve file paths eagerly (see the secrets section).

If your legacy setup relied on checked-in secret files, migrate them to CI secret variables. For GitHub Actions,
store file content as base64 in a secret and decode it in a pre-step (see the secrets section).

