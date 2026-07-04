[← Documentation](../../README.md)

# Plugins

Each integration is a separate Gradle plugin. Apply the **Foundation** plugin (it drives
variant-aware tag/version/changelog wiring that every other plugin relies on), then add only the
integrations you need.

| Plugin | Use it to… | Key tasks | Docs |
| --- | --- | --- | --- |
| **Foundation** | Derive version name/code from Git tags, rename outputs, generate changelogs | `getLastTagSnapshot<V>`, `generateChangelog<V>`, `printLastIncreasedTag<V>` | [foundation.md](./foundation.md) |
| **Firebase** | Upload builds to Firebase App Distribution | `appDistributionUpload<V>` | [firebase.md](./firebase.md) |
| **Play Store** | Publish app bundles to Google Play tracks | `playUpload<V>` | [play.md](./play.md) |
| **Slack** | Send changelogs and upload builds to Slack | `sendSlackChangelog<V>`, `slackDistributionUpload<V>` | [slack.md](./slack.md) |
| **Telegram** | Send changelogs and upload builds to Telegram (incl. self-hosted Bot API) | `sendTelegramChangelog<V>`, `telegramDistributionUpload<V>`, `telegramLookup` | [telegram.md](./telegram.md) |
| **Jira** | Automate fix versions, labels and status transitions from changelog issues | `jiraAutomation<V>` | [jira.md](./jira.md) |
| **Confluence** | Upload builds/changelogs to a Confluence page | `confluenceDistributionUpload<V>` | [confluence.md](./confluence.md) |
| **ClickUp** | Tag ClickUp tasks and set fix versions from changelog issues | `clickUpAutomation<V>` | [clickup.md](./clickup.md) |
| **Nextcloud** | Upload builds/changelogs to a Nextcloud folder with sharing | `nextcloudDistributionUpload<V>`, `nextcloudChangelogUpload<V>` | [nextcloud.md](./nextcloud.md) |

`<V>` is the capitalized build variant (e.g. `generateChangelogRelease`).

> **Non-Android / standalone automation?** The Android-free
> **`ru.kode.android.build-publish-novo.sender`** plugin aggregates every integration's CLI task
> behind a single `buildPublishSender { … }` extension — see [standalone tasks](../standalone-tasks.md).

## How it works & how to set up

- New here? Read [Tag-based automation (core concepts)](../concepts/tag-based-automation.md) first.
- [Installation](../installation.md) — repositories, applying plugins, convention/build-logic setup.
- [Configuring secrets](../secrets.md) — tokens and passwords via environment variables.
- Upgrading? [Migration guides](../migration/index.md) (including the **[2.0 breaking changes](../migration/v2.md)**).
