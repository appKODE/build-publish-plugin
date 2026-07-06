# Android Build Publish Plugin

A modular Gradle plugin suite that automates Android build publishing workflows: Git-tag-based
version management, changelog generation, and distribution/notification integrations (Firebase, Play
Store, Slack, Telegram, Jira, Confluence, ClickUp, Nextcloud).

> **Upgrading?** 2.0 contains breaking DSL changes — see the
> **[2.0 migration guide](docs/migration/v2.md)**. For the 2.0 → 2.1 changes (Jira multi-instance
> registry, changelog issue resolution) see the **[2.1 migration guide](docs/migration/v2.1.md)**.

## Why this suite

Designed to be "build friendly" and behave well in CI/CD:

- **Lazy, configuration-avoidant** — values are `Property`/`Provider`-backed and resolved late; tasks
  are registered without being realized unless needed.
- **Conditional tasks** — integration tasks are created only when their configuration is present
  (e.g. `jiraAutomation<V>` only when an automation action is enabled).
- **No network at configuration time** — network calls happen only during task execution, via the
  Gradle Worker API, backed by shared build services (one HTTP client per integration).
- **Variant-aware** — tasks are wired per Android build variant with a predictable naming scheme.

More detail: [Design & CI-friendliness](docs/concepts/design-and-efficiency.md).

## Quick start

Apply the **Foundation** plugin (required — it drives tag/version/changelog wiring) plus any
integrations you need:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.telegram") // add integrations as needed
}

buildPublishFoundation {
    output { common { baseFileName.set("my-app") } }
    changelog {
        common {
            commitMessageKey.set("CHANGELOG")
            issueSource("tracker") {
                numberPattern.set("TICKET-\\d+")
                urlPrefix.set("https://jira.example.com/browse/")
            }
        }
    }
}
```

Then see [Installation](docs/installation.md) for repositories, version catalogs, and convention
(`build-logic`) setup, and the [plugin docs](docs/plugins/index.md) for per-integration configuration.

## Documentation

- **[Tag-based automation (core concepts)](docs/concepts/tag-based-automation.md)** — how tags drive
  versions and changelogs.
- **[Design & CI-friendliness](docs/concepts/design-and-efficiency.md)** — lazy config, Worker API,
  shared services, variant-aware wiring.
- **[Installation](docs/installation.md)** — repositories, applying plugins, convention plugins.
- **[Configuring secrets](docs/secrets.md)** — tokens/passwords via environment variables.
- **[Plugins](docs/plugins/index.md)** — one page per integration, with a decision matrix:
  [Foundation](docs/plugins/foundation.md) ·
  [Firebase](docs/plugins/firebase.md) ·
  [Play Store](docs/plugins/play.md) ·
  [Slack](docs/plugins/slack.md) ·
  [Telegram](docs/plugins/telegram.md) ·
  [Jira](docs/plugins/jira.md) ·
  [Confluence](docs/plugins/confluence.md) ·
  [ClickUp](docs/plugins/clickup.md) ·
  [Nextcloud](docs/plugins/nextcloud.md)
- **[Reusable client libraries & standalone tasks](docs/standalone-tasks.md)** — the published
  `build-publish-novo-client-*` libraries and CLI-invokable tasks (incl. the Android-free `sender`).
- **[Custom plugin development](docs/custom-plugins.md)** — extend variant configuration with your own plugin.
- **[Troubleshooting](docs/troubleshooting.md)**
- **[Migration guides](docs/migration/index.md)** — [legacy → novo](docs/migration/legacy-to-novo.md),
  **[novo 1.x → 2.0](docs/migration/v2.md)** and **[novo 2.0 → 2.1](docs/migration/v2.1.md)**.
- **[Roadmap](ROADMAP.md)** — recently shipped work, planned themes, and what is intentionally out of scope.

## Examples

The repository ships runnable references:

- **`example-project/`** — a complete Android app using the plugins across multiple build types and
  flavors. Try `./gradlew tasks`, then `./gradlew assembleDebug` / `assembleRelease`.
- **`example-plugin/`** — a minimal custom plugin that participates in variant configuration.
- **`plugin-test/`** — integration tests exercising every plugin; useful as practical, working
  configuration references.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
