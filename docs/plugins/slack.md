[← Documentation](../../README.md) · [All plugins](./index.md)

# Slack plugin

`ru.kode.android.build-publish-novo.slack`

Send build notifications to Slack channels with detailed build information.

#### Key Features
- Send build notifications to Slack
- Customizable message templates
- Support for multiple channels
- Build status and download links
- Changelog preview

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `sendSlackChangelog<Variant>` | Sends the generated changelog to Slack | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |
| `slackDistributionUpload<Variant>` | Uploads APK to Slack channels | `getLastTagSnapshot<Variant>` |
| `slackDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to Slack channels | `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Send changelog to Slack
./gradlew sendSlackChangelogRelease

# Upload APK to Slack
./gradlew slackDistributionUploadDebug
```

#### Minimum Setup

1. Create a Slack webhook URL:
   - Go to https://api.slack.com/apps
   - Create a new app and enable Incoming Webhooks
   - Add the webhook to your workspace

2. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.slack")
   }
   
   buildPublishSlack {
       bot {
           common {
               webhookUrl.set("https://hooks.slack.com/services/...")
               uploadApiTokenFile.set(file("slack-upload-token.txt"))
               iconUrl.set("https://example.com/bot.png")
           }
       }

       distribution {
           common {
               destinationChannel("#releases")
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
    id 'ru.kode.android.build-publish-novo.slack'
}

buildPublishSlack {
    bot {
        common {
            webhookUrl.set('https://hooks.slack.com/services/...')
            uploadApiTokenFile.set(file('slack-upload-token.txt'))
            iconUrl.set('https://example.com/bot.png')
        }
    }

    distribution {
        common {
            destinationChannel('#releases')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishSlack {
    bot {
        common {
            webhookUrl.set("https://hooks.slack.com/services/...")
            uploadApiTokenFile.set(file("slack-upload-token.txt"))
            iconUrl.set("https://example.com/bot.png")
        }

        buildVariant("release") {
            webhookUrl.set("https://hooks.slack.com/services/...")
            uploadApiTokenFile.set(file("slack-upload-token.txt"))
            iconUrl.set("https://example.com/release-bot.png")
        }
    }

    changelog {
        common {
            attachmentColor.set("#36a64f")
            userMention("@here")
        }

        buildVariant("release") {
            attachmentColor.set("#3aa3e3")
            userMentions("@channel")
        }
    }

    distribution {
        common {
            destinationChannels("#releases")
        }

        buildVariant("debug") {
            destinationChannels("#android-team")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishSlack {
    bot {
        common {
            webhookUrl.set('https://hooks.slack.com/services/...')
            uploadApiTokenFile.set(file('slack-upload-token.txt'))
            iconUrl.set('https://example.com/bot.png')
        }

        buildVariant('release') {
            webhookUrl.set('https://hooks.slack.com/services/...')
            uploadApiTokenFile.set(file('slack-upload-token.txt'))
            iconUrl.set('https://example.com/release-bot.png')
        }
    }

    changelog {
        common {
            attachmentColor.set('#36a64f')
            userMention('@here')
        }

        buildVariant('release') {
            attachmentColor.set('#3aa3e3')
            userMentions('@channel')
        }
    }

    distribution {
        common {
            destinationChannels('#releases')
        }

        buildVariant('debug') {
            destinationChannels('#android-team')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Slack plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **A bot configuration is always required**
  - For each variant, Slack requires `bot.common { ... }` (internally `default`) or `bot.buildVariant("<name>") { ... }`.
  - If bot config is missing for a variant, configuration fails.

- **At least one of `changelog` or `distribution` must be configured**
  - If both are missing for a variant, configuration fails.

- **Distribution tasks are registered only when channels are configured**
  - `slackDistributionUpload<Variant>` / `slackDistributionUploadBundle<Variant>` are created only when `distribution { ... }` has at least one destination channel.

- **`uploadApiTokenFile` is required when you run distribution uploads**
  - The plugin may still register distribution tasks without a token file, but execution will fail when the Slack API token is missing.

- **Slack upload can time out but still succeed**
  - Slack’s API may return a timeout even if the file is uploaded successfully; the plugin logs a warning for this case.

##### Bot (`buildPublishSlack { bot { ... } }`)

Configure bot connection details per variant.

Properties (applies to each `SlackBotConfig`):

- **`webhookUrl`** *(required)*
  - **What it does**: Slack Incoming Webhook URL used to post changelog messages.
  - **Why you need it**: Used by `sendSlackChangelog<Variant>`.
  - **Where to get it**: Create/configure an app at https://api.slack.com/apps, enable **Incoming Webhooks**, then add a webhook to your workspace and copy the URL.

- **`uploadApiTokenFile`** *(required for file uploads)*
  - **What it does**: File containing a Slack bot/user token for file uploads.
  - **Why you need it**: Required by `slackDistributionUpload*` tasks.
  - **Where to get it**: In your Slack app settings, add required OAuth scopes (**`files:write` and `files:read`**), install (or re-install) the app to the workspace, then copy the **Bot User OAuth Token** (usually `xoxb-...`). Store the token in a local file.
  - **Notes / required Slack setup for uploads**:
    - **Add OAuth scopes**: Slack App settings -> **OAuth & Permissions** -> **Scopes** -> add:
      - `files:write`
      - `files:read`
      - Link: https://api.slack.com/scopes
    - **Reinstall the app after changing scopes**: after you change scopes, Slack requires re-installation for permissions to apply:
      - Slack App settings -> **Settings** -> **Install App** -> click **Reinstall to Workspace** (or **Install to Workspace**)
      - Link: https://api.slack.com/start/quickstart#installing
    - **Add the app/bot to the destination channel**: the bot must be a member of the target channel (the one you configure via `destinationChannel(...)` / `destinationChannels(...)`):
      - Open the channel in Slack UI
      - Channel name menu -> **Integrations** -> **Add apps** -> add your app/bot
      - Link: https://slack.com/help/articles/202035138-Add-apps-to-your-Slack-workspace

- **`iconUrl`** *(required for changelog messages)*
  - **What it does**: Icon URL for Slack message sender.
  - **Why you need it**: Used by `sendSlackChangelog<Variant>`.
  - **Where to get it**: Any publicly accessible image URL (for example a hosted PNG in your company CDN or a GitHub raw URL).

##### Changelog (`buildPublishSlack { changelog { ... } }`)

Properties (applies to each `SlackChangelogConfig`):

- **`attachmentColor`** *(required)*
  - **What it does**: Hex color used for Slack attachment stripe (e.g. `#36a64f`).
  - **Why you need it**: Helps visually identify message type.

- **`userMention(...)` / `userMentions(...)`**
  - **What it does**: Adds mentions (e.g. `@here`, `@channel`, `@username`) to the message.
  - **Why you need it**: Notifies specific people/groups about a release.

##### Distribution (`buildPublishSlack { distribution { ... } }`)

Properties (applies to each `SlackDistributionConfig`):

- **`destinationChannel(...)` / `destinationChannels(...)`** *(required to create upload tasks)*
  - **What it does**: Sets channels where artifacts will be shared.
  - **Common values**: `#releases`, `#android-team`.
  - **Where to get it**: Slack channel name as shown in the UI (including the leading `#`). Ensure your app/bot has access to post/upload in that channel.

##### Task options

- **`sendSlackChangelog<Variant>`** supports (CLI options):
  - `--changelogFile=/abs/path/to/changelog.md`
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--iconUrl=https://example.com/icon.png`
  - `--userMentions=@here`
  - `--attachmentColor=#36a64f`

- **`slackDistributionUpload<Variant>` / `slackDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--channels=#releases`

---

