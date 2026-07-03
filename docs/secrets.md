[← Documentation](../README.md)

# Configuring secrets via environment variables

For CI/CD it is recommended to configure credentials via **environment variables** and wire them into plugin
configuration using Gradle’s `ProviderFactory`:

The actual environment variable names are up to you — the plugin just consumes whatever value you pass into the DSL.
Examples in this README use names like `JIRA_API_TOKEN`, `CONFLUENCE_API_TOKEN`, `NEXTCLOUD_APP_PASSWORD`, etc.
You can standardize them however you prefer in your CI.

- `providers.environmentVariable("...")` is lazy (safe for configuration avoidance).
- You can validate values early and fail the build with a clear message.

#### 4.1) Where to store secrets (including files)

For security reasons:

- **Do not commit secrets** (tokens, passwords, service-account JSON, etc.) into the repository.
- **Local development**: store secrets in `local.properties` (gitignored) or environment variables.
- **CI/CD**: store secrets in your CI/CD secret variables store.

GitHub Actions does not support “secret files” directly. For files (for example JSON credentials), a common
approach is to store the file content in a secret as **base64** and decode it at runtime.

#### Kotlin DSL (`build.gradle.kts`)

String secret (for example, bot token):

```kotlin
val telegramBotIdProvider =
    providers.environmentVariable("TELEGRAM_CHANGELOGGER_BOT_ID")
        .map {
            if (isBlank()) {
                throw GradleException("no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports")
            }
            it
        }
        .orElse("")

buildPublishTelegram {
    botsCommon {
        bot("changelogger") {
            botId.set(telegramBotIdProvider)
        }
    }
}
```

File secret (env var contains file path):

```kotlin
buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(
                providers.environmentVariable("CLICKUP_TOKEN_FILE")
                    .map {
                        if (isBlank()) {
                            throw GradleException("no CLICKUP_TOKEN_FILE env var provided")
                        }
                        layout.projectDirectory.file(it)
                    }
            )
        }
    }
}
```

#### Groovy DSL (`build.gradle`)

```groovy
def telegramBotIdProvider = providers.environmentVariable('TELEGRAM_CHANGELOGGER_BOT_ID')
    .map {
        if (isBlank()) {
            throw new GradleException('no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports')
        }
        it
    }
    .orElse('')

buildPublishTelegram {
    botsCommon {
        bot('changelogger') {
            botId.set(telegramBotIdProvider)
        }
    }
}
```

#### Note about file-based token properties and Worker API isolation

Some integrations use Gradle Worker API / shared services under the hood. For **file-based secrets** (for example
Slack `uploadApiTokenFile`) it can be safer to resolve the environment variable **eagerly** into a concrete file
path and set the property to an actual file (instead of relying on lazy `Provider` mapping).

Groovy DSL example:

```groovy
// NOTE: Need to get it eagerly, because it cannot be resolved correctly in isolated environment
def slackApiTokenFilePath = System.getenv("SLACK_API_KEY") ?: "${rootProject.projectDir}/slack-token.txt"

buildPublishSlack {
    bot {
        common {
            uploadApiTokenFile.set(project.file(slackApiTokenFilePath))
        }
    }
}
```

Kotlin DSL example:

```kotlin
// NOTE: Need to get it eagerly, because it cannot be resolved correctly in isolated environment
val slackApiTokenFilePath = System.getenv("SLACK_API_KEY") ?: "${rootProject.projectDir}/slack-token.txt"

buildPublishSlack {
    bot {
        common {
            uploadApiTokenFile.set(project.file(slackApiTokenFilePath))
        }
    }
}
```

#### 4.2) GitHub Actions: store secret files as base64 and decode in a pre-step

Example (service account JSON stored in `PLAY_ACCOUNT_JSON_B64`):

1) Encode locally:

```bash
base64 -w 0 play-account.json
```

2) Save the resulting value in GitHub repository secrets (e.g. `PLAY_ACCOUNT_JSON_B64`).

3) Decode it in workflow before Gradle runs:

```yaml
- name: Decode Play service account
  shell: bash
  run: |
    echo "${{ secrets.PLAY_ACCOUNT_JSON_B64 }}" | base64 --decode > play-account.json
```

Then reference the generated file from Gradle, for example:

```kotlin
buildPublishPlay {
    auth {
        common {
            apiTokenFile.set(file("play-account.json"))
        }
    }
}
```

