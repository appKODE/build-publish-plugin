# Android Build Publish Plugin

A comprehensive Gradle plugin suite for automating Android build publishing workflows. This plugin provides:

- Version management through Git tags
- Automated changelog generation
- Firebase App Distribution
- Google Play Store publishing
- Jira integration
- Telegram notifications
- Custom plugin support

## Table of Contents

- [Installation](#installation)
- [Available Plugins](#available-plugins)
   - [Foundation Plugin](#1-foundation-plugin)
   - [Firebase Plugin](#2-firebase-plugin)
   - [Play Store Plugin](#3-play-store-plugin)
   - [Slack Plugin](#4-slack-plugin)
   - [Telegram Plugin](#5-telegram-plugin)
   - [Jira Plugin](#6-jira-plugin)
   - [Confluence Plugin](#7-confluence-plugin)
   - [ClickUp Plugin](#8-clickup-plugin)
- [Custom Plugin Development](#custom-plugin-development)
- [Version Management](#version-management)
- [Common Tasks](#common-tasks)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Installation

Add the plugin to your project's root `build.gradle.kts`:

```kotlin
// Top-level build.gradle.kts
plugins {
   id("ru.kode.android.build.publish") version "x.y.z"
}

// Required repositories
dependencyResolutionManagement {
   repositories {
      google()
      mavenCentral()
      // Add this if you're using a custom Maven repository
      // maven { url = uri("https://your-maven-repo.com") }
   }
}
```

## Available Plugins

### 1. Foundation Plugin (`ru.kode.android.build.publish.foundation`)

The core plugin that provides essential functionality for build publishing, version management, and changelog generation. This plugin must be applied to all modules that will use any of the publishing plugins.

#### Key Features
- Automatic version management using Git tags
- Changelog generation from commit history
- Build variant support (flavors and build types)
- Customizable version code and name strategies
- Support for multiple output formats (APK, AAB)

#### Minimum Setup

```kotlin
// app/build.gradle.kts
plugins {
   id("com.android.application")
   id("ru.kode.android.build.publish.foundation")
}

buildPublishFoundation {
   output {
      common {
         useVersionsFromTag.set(true)
         buildTagPattern.set("v%s-%s") // Format: v{version}-{variant}
      }
   }
}
```

#### Full Configuration

```kotlin
buildPublishFoundation {
   output {
      common {
         useVersionsFromTag.set(true)
         buildTagPattern.set("v%s-%s") // Format: v{version}-{variant}
         useDefaultsForVersionsAsFallback.set(true)
      }

      variant("debug") {
         useStubsForTagAsFallback.set(true)
      }
   }

   changelog {
      common {
         issueNumberPattern.set("#(\\d+)")
         issueUrlPrefix.set("https://your-issue-tracker.com/issue/")
         commitMessageKey.set("message")
         excludeMessageKey.set(true)
      }
   }
}
```

#### Key Features
- Version management through Git tags
- Changelog generation from commit history
- Build variant support
- Customizable version name and code strategies

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `getLastTag<Variant>` | Retrieves the last Git tag for the build variant | - |
| `printLastIncreasedTag<Variant>` | Prints the last increased version tag | `getLastTag<Variant>` |
| `generateChangelog<Variant>` | Generates changelog from Git history | `getLastTag<Variant>` |
| `renameApk<Variant>` | Renames APK files with version information | `getLastTag<Variant>` |

#### Task Usage Examples

```bash
# Get last tag for debug variant
./gradlew getLastTagDebug

# Generate changelog for release variant
./gradlew generateChangelogRelease

# Print last increased tag for staging variant
./gradlew printLastIncreasedTagStaging
```

---

### 2. Firebase Plugin (`ru.kode.android.build.publish.firebase`)

Publish builds to Firebase App Distribution with support for multiple variants and tester groups.

#### Key Features
- Publish APK/AAB to Firebase App Distribution
- Support for multiple build variants
- Tester group management
- Release notes from changelog
- Integration with Firebase service accounts

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `publishToFirebase<Variant>` | Publishes the build to Firebase App Distribution | `bundle<Variant>` or `assemble<Variant>` |
| `publishToFirebase` | Publishes all variants to Firebase | All `publishToFirebase<Variant>` tasks |

#### Task Usage Examples

```bash
# Publish debug build to Firebase
./gradlew publishToFirebaseDebug

# Publish release build to Firebase with custom notes
./gradlew publishToFirebaseRelease --notes="New features and bug fixes"
```

#### Minimum Setup

1. Add Firebase configuration to your project:
   - Add `google-services.json` to your app module
   - Add Firebase App Distribution plugin to your root `build.gradle.kts`:
     ```kotlin
     plugins {
         id("com.google.firebase.appdistribution") version "4.0.0"
     }
     ```

2. Configure the plugin:
   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build.publish.foundation")
       id("ru.kode.android.build.publish.firebase")
   }
   
   buildPublishFirebase {
       distribution {
           common {
               appId.set("your-firebase-app-id")
               serviceCredentialsFile.set(file("path/to/service-account.json"))
               artifactType.set(ArtifactType.Bundle)
           }
       }
   }
   ```

#### Full Configuration

```kotlin
buildPublishFirebase {
   distribution {
      common {
         appId.set("your-firebase-app-id")
         serviceCredentialsFile.set(file("path/to/service-account.json"))
         artifactType.set(ArtifactType.Bundle)
         testerGroup("qa-team")
         testerGroup("developers")
      }

      variant("release") {
         testerGroup("beta-testers")
      }
   }
}
```

#### Available Properties
- `appId`: Firebase App ID (required)
- `serviceCredentialsFile`: Path to service account JSON (required)
- `artifactType`: `ArtifactType.Apk` or `ArtifactType.Bundle`
- `testerGroup`: Add tester groups to receive the build

---

### 3. Play Store Plugin (`ru.kode.android.build.publish.play`)

Publish builds to Google Play Store with support for multiple tracks and release types.

#### Key Features
- Publish to Google Play Console
- Support for multiple tracks (internal, alpha, beta, production)
- Release management (draft, in progress, completed)
- Support for release notes in multiple languages
- Integration with Google Play service account

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `publishToPlay<Variant>` | Uploads the build to Google Play Console | `bundle<Variant>` |
| `publishToPlay` | Uploads all variants to Google Play | All `publishToPlay<Variant>` tasks |
| `promoteToAlpha<Variant>` | Promotes build to alpha track | `publishToPlay<Variant>` |
| `promoteToBeta<Variant>` | Promotes build to beta track | `publishToPlay<Variant>` |
| `promoteToProduction<Variant>` | Promotes build to production track | `publishToPlay<Variant>` |

#### Task Usage Examples

```bash
# Upload release bundle to internal testing track
./gradlew publishToPlayRelease

# Promote to beta track
./gradlew promoteToBetaRelease

# Upload with specific track and status
./gradlew publishToPlayRelease --track=production --status=completed
```

#### Minimum Setup

1. Create a service account in Google Play Console
2. Download the JSON key file and add it to your project
3. Configure the plugin:
   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build.publish.foundation")
       id("ru.kode.android.build.publish.play")
   }
   
   buildPublishPlay {
       publish {
           common {
               serviceAccountCredentials.set(file("play-account.json"))
               track.set("internal")
           }
       }
   }
   ```

#### Full Configuration

```kotlin
buildPublishPlay {
   publish {
      common {
         serviceAccountCredentials.set(file("play-account.json"))
         track.set("internal")
         releaseStatus.set("draft")
      }
   }
}
```

#### Available Properties
- `serviceAccountCredentials`: Play Store API credentials
- `track`: Target track (internal, alpha, beta, production)
- `releaseStatus`: Release status (draft, completed, etc.)

---

### 4. Slack Plugin (`ru.kode.android.build.publish.slack`)

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
| `notifySlack<Variant>` | Sends build notification to Slack | `assemble<Variant>` |
| `notifySlack` | Notifies Slack for all variants | All `notifySlack<Variant>` tasks |

#### Task Usage Examples

```bash
# Send notification for debug build
./gradlew notifySlackDebug

# Send custom message to specific channel
./gradlew notifySlackRelease --message="New release is ready!" --channel="#releases"
```

#### Minimum Setup

1. Create a Slack webhook URL:
   - Go to https://api.slack.com/apps
   - Create a new app and enable Incoming Webhooks
   - Add the webhook to your workspace

2. Configure the plugin:
   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build.publish.foundation")
       id("ru.kode.android.build.publish.slack")
   }
   
   buildPublishSlack {
       webhook {
           common {
               webhookUrl.set("https://hooks.slack.com/services/...")
               channel.set("#releases")
           }
       }
   }
   ```

#### Full Configuration

```kotlin
buildPublishSlack {
   webhook {
      common {
         webhookUrl.set("https://hooks.slack.com/...")
         channel.set("#releases")
         message.set("New build available!")
      }
   }
}
```

#### Available Properties
- `webhookUrl`: Slack webhook URL
- `channel`: Channel to post to
- `message`: Custom notification message

---

### 5. Telegram Plugin (`ru.kode.android.build.publish.telegram`)

Send build notifications to Telegram channels or groups.

#### Key Features
- Send build notifications to Telegram
- Support for both public and private channels
- Custom message formatting
- Download links and build information
- Changelog preview

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `notifyTelegram<Variant>` | Sends build notification to Telegram | `assemble<Variant>` |
| `notifyTelegram` | Notifies Telegram for all variants | All `notifyTelegram<Variant>` tasks |

#### Task Usage Examples

```bash
# Send notification for debug build
./gradlew notifyTelegramDebug

# Custom message with build info
./gradlew notifyTelegramRelease --message="🚀 New version %VERSION% is ready!"
```

#### Minimum Setup

1. Create a Telegram bot:
   - Message @BotFather on Telegram
   - Use `/newbot` command and follow instructions
   - Get your bot token

2. Get your chat ID:
   - Add your bot to a channel/group
   - Send a message to the channel/group
   - Visit `https://api.telegram.org/bot<your-bot-token>/getUpdates`
   - Find the chat ID in the response

3. Configure the plugin:
   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build.publish.foundation")
       id("ru.kode.android.build.publish.telegram")
   }
   
   buildPublishTelegram {
       bot {
           common {
               botToken.set("your-bot-token")
               chatId.set("@your-channel")
           }
       }
   }
   ```

#### Full Configuration

```kotlin
buildPublishTelegram {
   bot {
      common {
         botToken.set("your-bot-token")
         chatId.set("@your-channel")
         message.set("New build available for testing!")
      }
   }
}
```

#### Available Properties
- `botToken`: Telegram bot token
- `chatId`: Target chat/channel ID
- `message`: Custom notification message

---

### 6. Jira Plugin (`ru.kode.android.build.publish.jira`)

Update Jira tickets with build information.

#### Configuration

```kotlin
buildPublishJira {
   integration {
      common {
         baseUrl.set("https://your-domain.atlassian.net")
         username.set("your-email@example.com")
         apiToken.set("your-api-token")
         projectKey.set("PROJ")
      }
   }
}
```

#### Available Properties
- `baseUrl`: Jira instance URL
- `username`: Jira username/email
- `apiToken`: Jira API token
- `projectKey`: Jira project key

---

### 7. Confluence Plugin (`ru.kode.android.build.publish.confluence`)

Update Confluence pages with release notes.

#### Configuration

```kotlin
buildPublishConfluence {
   integration {
      common {
         baseUrl.set("https://your-domain.atlassian.net/wiki")
         username.set("your-email@example.com")
         apiToken.set("your-api-token")
         spaceKey.set("REL")
         parentPageId.set("12345678")
      }
   }
}
```

#### Available Properties
- `baseUrl`: Confluence base URL
- `username`: Confluence username/email
- `apiToken`: Confluence API token
- `spaceKey`: Target space key
- `parentPageId`: Parent page ID for release notes

---

### 8. ClickUp Plugin (`ru.kode.android.build.publish.clickup`)

Update ClickUp tasks with build information.

#### Configuration

```kotlin
buildPublishClickUp {
   integration {
      common {
         apiKey.set("your-clickup-api-key")
         teamId.set("your-team-id")
         listId.set("your-list-id")
      }
   }
}
```

#### Available Properties
- `apiKey`: ClickUp API key
- `teamId`: ClickUp team ID
- `listId`: Target list ID for task creation

## Troubleshooting

### Common Issues

#### Firebase Authentication Errors
```
> Failed to authenticate with Firebase
```
**Solution:**
1. Verify your `google-services.json` is in the correct location
2. Ensure the service account has the necessary permissions in Firebase Console
3. Check that the `appId` in your configuration matches your Firebase project

#### Play Store Upload Failures
```
> Failed to upload to Play Store: 403 Forbidden
```
**Solution:**
1. Verify your service account has the correct permissions in Google Play Console
2. Check that the package name in your app matches the one in Play Console
3. Ensure the service account is added to your app in Play Console

#### Changelog Generation Issues
```
> No Git tags found for changelog generation
```
**Solution:**
1. Make sure you have at least one Git tag
2. Verify your tag format matches the pattern in `buildTagPattern`
3. Run `git fetch --tags` to ensure all tags are available locally

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Setup

1. Clone the repository
2. Open in Android Studio or your favorite IDE
3. Run `./gradlew build` to build the project
4. Use `./gradlew publishToMavenLocal` to test your changes locally

## License

Distributed under the MIT License. See `LICENSE` for more information.

## Version Management

The plugin supports semantic versioning through Git tags:

```
v{version}-{variant}
```

Example: `v1.2.3-debug`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the [MIT License](LICENSE).
