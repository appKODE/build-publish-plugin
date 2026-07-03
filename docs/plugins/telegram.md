[← Documentation](../../README.md) · [All plugins](./index.md)

# Telegram plugin

`ru.kode.android.build-publish-novo.telegram`

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
| `sendTelegramChangelog<Variant>` | Sends generated changelog to Telegram | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |
| `telegramDistributionUpload<Variant>` | Sends APK distribution notification to Telegram | - |
| `telegramDistributionUploadBundle<Variant>` | Sends bundle distribution notification to Telegram | - |
| `telegramLookup<Variant>` | Helps discover chat/topic identifiers via configured bot | - |

#### Task Usage Examples

```bash
# Send changelog
./gradlew sendTelegramChangelogRelease

# Lookup chat/topic IDs (if lookup is configured)
./gradlew telegramLookupDebug
```

#### Minimum Setup

1. Create a Telegram bot if it is not exists:
   - Message @BotFather on Telegram
   - Use `/newbot` command and follow instructions
   - Get your bot token

2. Discover `chatId` and `topicId` using `telegramLookup<Variant>`:
   - Add your bot to the target channel/group/topic
   - Send **any** message to that chat/topic from your Telegram account
   - Configure `buildPublishTelegram.lookup { ... }` (see Full Configuration below) with:
     - `botName` = your configured bot name (for example `main`)
     - `chatName` = part of the chat title to match
     - `topicName` = (optional) part of the topic title to match
   - Run the lookup task, for example:
     - `./gradlew telegramLookupDebug`
   - The task prints `Chat ID` and `Topic ID` in the build output. Copy those values into:
     - `bots { ... bot("...") { chat("...") { chatId.set("..."); topicId.set("...") } } }`

3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.telegram")
   }
   
   buildPublishTelegram {
       bots {
           common {
               bot("main") {
                   botId.set("your-bot-token")
                   chat("releases") {
                       chatId.set("@your-channel")
                   }
               }
           }
       }

       changelog {
           common {
               destinationBot {
                   botName.set("main")
                   chatName("releases")
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
    id 'ru.kode.android.build-publish-novo.telegram'
}

buildPublishTelegram {
    bots {
        common {
            bot('main') {
                botId.set('your-bot-token')

                chat('releases') {
                    chatId.set('@your-channel')
                }
            }
        }
    }

    changelog {
        common {
            destinationBot {
                botName.set('main')
                chatName('releases')
            }
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishTelegram {
    bots {
        common {
            bot("main") {
                botId.set("your-bot-token")
              
                // Optional: for self-hosted Bot API
                // botServerBaseUrl.set('https://telegram-bot-api.your-company.net')
                // botServerAuth.username.set(providers.environmentVariable('TELEGRAM_AUTH_USER'))
                // botServerAuth.password.set(providers.environmentVariable('TELEGRAM_AUTH_PASSWORD'))
  
                chat("releases") {
                      chatId.set("@your-channel")
                      topicId.set("123")
                }
            }
        }
    }

    // Optional helper task configuration (enables telegramLookup<Variant>)
    lookup {
        botName.set("main")
        chatName.set("releases")
        topicName.set("Android releases")
    }

    changelog {
        common {
            userMention("@dev1")
            destinationBot {
                botName.set("main")
                chatName("releases")
            }
        }
    }

    distribution {
        common {
            compressed.set(true)
            destinationBot {
                botName.set("main")
                chatName("releases")
            }
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishTelegram {
    bots {
        common {
            bot('main') {
                botId.set('your-bot-token')

                // Optional: for self-hosted Bot API
                // botServerBaseUrl.set('https://telegram-bot-api.your-company.net')
                // botServerAuth.username.set(providers.environmentVariable('TELEGRAM_AUTH_USER'))
                // botServerAuth.password.set(providers.environmentVariable('TELEGRAM_AUTH_PASSWORD'))

                chat('releases') {
                    chatId.set('@your-channel')
                    topicId.set('123')
                }
            }
        }
    }

    // Optional helper task configuration (enables telegramLookup<Variant>)
    lookup {
        botName.set('main')
        chatName.set('releases')
        topicName.set('Android releases')
    }

    changelog {
        common {
            userMention('@dev1')
            userMentions('@qa_team')

            destinationBot {
                botName.set('main')
                chatName('releases')
            }
        }
    }

    distribution {
        common {
            compressed.set(true)
            destinationBot {
                botName.set('main')
                chatName('releases')
            }
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Telegram plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **`bots { ... }` is always required**
  - If no bots are configured, plugin configuration fails.

- **At least one of `changelog`, `distribution`, or `lookup` must be configured**
  - If all three blocks are missing, plugin configuration fails.

- **Distribution tasks are registered only when destination bots exist**
  - `telegramDistributionUpload<Variant>` / `telegramDistributionUploadBundle<Variant>` are registered only when `distribution { ... }` has at least one `destinationBot { ... }`.

- **`telegramLookup<Variant>` exists only when `lookup { ... }` is configured**

##### Bots (`buildPublishTelegram { bots { ... } }`)

The Telegram plugin uses a two-level DSL:

1. **Select a build variant** via `common { ... }` or `buildVariant("release") { ... }`.
2. Inside that, register one or more bots with `bot("<name>") { ... }`.

Properties (applies to each `TelegramBotConfig`):

- **`botId`** *(required)*
  - **What it does**: Telegram bot token.
  - **Why you need it**: Used to authenticate Bot API calls.
  - **How to get**: Create bot via `@BotFather`.

- **`botServerBaseUrl`** *(optional)*
  - **What it does**: Base URL for a self-hosted Telegram Bot API server.
  - **Why you need it**: Only if you’re not using the official `https://api.telegram.org`.
  - **When it can be needed**:
    - If you need to upload large artifacts (for example, `.apk`/`.aab` files that are bigger than typical Bot API limits).
    - Telegram’s official Bot API docs explicitly state that a local Bot API server enables:
      - Uploads up to **2000 MB**
      - Downloads without a size limit
    - Docs: https://core.telegram.org/bots/api#using-a-local-bot-api-server
    - Server implementation (TDLib): https://github.com/tdlib/telegram-bot-api

- **`botServerAuth.username` / `botServerAuth.password`** *(optional)*
  - **What it does**: HTTP Basic Auth credentials for a protected self-hosted Bot API server.
  - **Why you need it**: Only if your Bot API endpoint requires basic auth.

Chats (inside `bot("...") { chat("...") { ... } }`):

- **`chatId`** *(required)*
  - **What it does**: Telegram chat identifier.
  - **Typical values**: `@channelusername`, `-1001234567890`, `123456789`.
  - **How to get**:
    - Public channel: use its username (for example `@your_channel`).
    - Private groups/channels or topics: use the `telegramLookup<Variant>` helper task described above to print the numeric `chatId` / `topicId`.

- **`topicId`** *(optional)*
  - **What it does**: Thread/topic id in a forum-style chat.
  - **Why you need it**: To send messages to a specific topic.
  - **How to get**: Use `telegramLookup<Variant>` (topics exist only for forum-style group chats).

##### Changelog (`buildPublishTelegram { changelog { ... } }`)

Properties (applies to each `TelegramChangelogConfig`):

- **`userMention(...)` / `userMentions(...)`**
  - **What it does**: Adds mentions (for example `@dev1`) to the message.
  - **Why you need it**: Notifies specific users.

- **`destinationBot { ... }`** *(required to actually send changelog)*
  - **What it does**: Selects which configured bot sends, and which named chats receive messages.
  - **How to use**:
    - `botName.set("main")`
    - `chatName("releases")` (repeat or use `chatNames(...)` for multiple)

##### Distribution (`buildPublishTelegram { distribution { ... } }`)

Properties (applies to each `TelegramDistributionConfig`):

- **`destinationBot { ... }`** *(required to create upload tasks)*
  - **What it does**: Selects bot + chats where the artifact will be uploaded.
  - **Note**: The task uploads the file you provide (APK or AAB) to all configured destinations.

- **`compressed`** *(optional, default `false`)*
  - **What it does**: Compresses the distribution file to a `.zip` before upload.
  - **Why you might need it**: Can reduce upload time for large artifacts.

##### Lookup (`buildPublishTelegram { lookup { ... } }`)

Lookup is an optional helper task to debug/verify your bot/chat/topic configuration.

Properties (applies to `TelegramLookupConfig`):

- **`botName`** *(required)*
- **`chatName`** *(required)*
- **`topicName`** *(optional)*

##### Task options

- **`sendTelegramChangelog<Variant>`** supports (CLI options):
  - `--changelogFile=/abs/path/to/changelog.md`
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--userMentions=@dev1`
  - `--destinationBots=<json>`

- **`telegramDistributionUpload<Variant>` / `telegramDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--destinationBots=<json>`
  - `--compressed=true`

- **`telegramLookup<Variant>`** supports (CLI options):
  - `--botName=main`
  - `--chatName=releases`
  - `--topicName=Android releases`

---

