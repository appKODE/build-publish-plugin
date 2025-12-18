package ru.kode.android.build.publish.plugin.slack.messages

fun servicesCreatedMessages(servicesNames: Set<String>): String {
    return """
        |
        |============================================================
        |       🚀 SLACK SERVICES INITIALIZED SUCCESSFULLY 🚀
        |============================================================
        | The following Slack services have been created and configured:
        |
        | ${servicesNames.joinToString("\n") { "| • $it" }}
        |
        | These services will be used for sending notifications and
        | managing communications with Slack workspaces.
        |============================================================
    """.trimMargin()
}

fun registeringServicesMessage(): String {
    return """
        |
        |============================================================
        |           🔄 REGISTERING SLACK SERVICES... 🔄
        |============================================================
        | Initializing and configuring Slack services...
        | This may take a moment as we set up the necessary
        | connections and configurations.
        |============================================================
    """.trimMargin()
}

fun noBotsConfiguredMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ NO SLACK BOTS CONFIGURED ℹ️
        |============================================================
        | No Slack bots have been configured in your build script.
        | The service map will remain empty.
        |
        | TO ENABLE SLACK NOTIFICATIONS:
        | 1. Add a 'slack' configuration block to your build script
        | 2. Define at least one bot with its API token and channels
        |
        | Example:
        | slack {
        |     bots {
        |         common {
        |             bot("myBot") {
        |                 token.set(providers.gradleProperty("slack.token"))
        |                 channel("#releases") {
        |                     id.set("C1234567890")
        |                 }
        |             }
        |         }
        |     }
        | }
        |============================================================
    """.trimMargin()
}

fun mustApplyFoundationPluginMessage(): String {
    return """
        |
        |============================================================
        |              🚨 PLUGIN CONFIGURATION ERROR 🚨
        |============================================================
        | The Slack plugin requires the BuildPublishFoundationPlugin
        | to be applied first.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        | }
        |
        | Make sure to replace <version> with the correct version number.
        |============================================================
    """.trimMargin()
}

fun extensionNotCreatedMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ SLACK EXTENSION INITIALIZED ℹ️
        |============================================================
        | SlackServiceExtension has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        | 1. Configure your Slack settings in the build script
        | 2. Define your bots and channels
        | 3. Apply the configuration using the 'slack' extension
        |
        | Example configuration:
        | slack {
        |     bots { ... }
        |     distribution { ... }
        |     changelog { ... }
        | }
        |============================================================
    """.trimMargin()
}

fun bundleDistributionNotCreatedMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ BUNDLE DISTRIBUTION SKIPPED ℹ️
        |============================================================
        | The Slack distribution task for Android App Bundles was not
        | created because required configuration is missing.
        |
        | MISSING CONFIGURATION:
        | • uploadApiTokenFile: Path to the file containing the Slack API token
        | • uploadChannels: List of channel IDs to upload to
        |
        | TO ENABLE BUNDLE DISTRIBUTION:
        | Add the following to your build script:
        |
        | slack {
        |     distribution {
        |         bundle {
        |             uploadApiTokenFile = file("path/to/token")
        |             uploadChannels = listOf("#releases")
        |         }
        |     }
        | }
        |============================================================
    """.trimMargin()
}

fun apkDistributionNotCreatedMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ APK DISTRIBUTION SKIPPED ℹ️
        |============================================================
        | The Slack distribution task for APK files was not created
        | because required configuration is missing.
        |
        | MISSING CONFIGURATION:
        | • uploadApiTokenFile: Path to the file containing the Slack API token
        | • uploadChannels: List of channel IDs to upload to
        |
        | TO ENABLE APK DISTRIBUTION:
        | Add the following to your build script:
        |
        | slack {
        |     distribution {
        |         apk {
        |             uploadApiTokenFile = file("path/to/token")
        |             uploadChannels = listOf("#beta-testers")
        |         }
        |     }
        | }
        |============================================================
    """.trimMargin()
}

fun uploadFailedMessage(): String {
    return """
        |
        |============================================================
        |                ⚠️ SLACK UPLOAD TIMEOUT ⚠️
        |============================================================
        | The file upload to Slack timed out, but it might have been
        | successful. This is a known issue with the Slack API.
        |
        | POSSIBLE CAUSES:
        | • Network latency or connectivity issues
        | • Large file size
        | • Slack API rate limiting
        |
        | RECOMMENDED ACTIONS:
        | 1. Check your Slack channel to verify if the file was uploaded
        | 2. If the file is present, you can safely ignore this warning
        | 3. If the file is missing, try uploading again
        |
        | For more information, see:
        | https://github.com/slackapi/python-slack-sdk/issues/1165
        |============================================================
    """.trimMargin()
}

fun changelogFileNotFoundMessage(): String {
    return """
        |
        |============================================================
        |          ❌ CHANGELOG FILE NOT FOUND OR EMPTY ❌
        |============================================================
        | The changelog file could not be found or is empty.
        |
        | POSSIBLE CAUSES:
        | 1. The changelog file was not generated
        | 2. The file path is incorrect
        | 3. The file exists but is empty
        | 4. Insufficient permissions to read the file
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify the changelog file exists at the expected location
        | 2. Check file permissions
        | 3. Ensure the changelog generation task ran successfully
        | 4. Verify the file is not empty
        |
        | If this is expected, you can ignore this message.
        |============================================================
    """.trimMargin()
}

fun changelogSentMessage(): String {
    return """
        |
        |============================================================
        |         ✅ CHANGELOG SENT TO SLACK SUCCESSFULLY ✅
        |============================================================
        | The changelog has been successfully sent to the configured
        | Slack channel(s).
        |
        | NEXT STEPS:
        | 1. Check your Slack channel to verify the changelog
        | 2. Consider pinning the message for better visibility
        |
        | If you don't see the message, please check:
        | - The bot has permission to post in the channel
        | - The channel ID is correct
        | - The message isn't marked as spam by Slack
        |============================================================
    """.trimMargin()
}

fun provideChangelogOrDistributionConfigMessage(buildVariant: String): String {
    return """
        |
        |============================================================
        |           ⚠️ MISSING REQUIRED CONFIGURATION ⚠️
        |============================================================
        | No valid configuration found for build variant: $buildVariant
        |
        | REQUIRED ACTION:
        | You must provide at least one of the following configurations:
        | 1. A changelog configuration
        | 2. A distribution configuration
        |
        | EXAMPLE CONFIGURATION:
        | slack {
        |     // For variant-specific configuration
        |     $buildVariant {
        |         changelog { ... }
        |         // OR
        |         distribution { ... }
        |     }
        |     
        |     // Or for common configuration
        |     common {
        |         changelog { ... }
        |         // OR
        |         distribution { ... }
        |     }
        | }
        |
        | For more details, check the plugin documentation.
        |============================================================
    """.trimMargin()
}

fun provideBotConfigMessage(buildVariant: String): String {
    return """
        |
        |============================================================
        |              ⚠️ MISSING BOT CONFIGURATION ⚠️
        |============================================================
        | No bot configuration found for build variant: $buildVariant
        |
        | REQUIRED ACTION:
        | You must configure at least one bot in your build script.
        |
        | EXAMPLE CONFIGURATION:
        | slack {
        |     // For variant-specific bot
        |     $buildVariant {
        |         bots {
        |             bot("myBot") {
        |                 token.set(providers.gradleProperty("slack.token"))
        |                 channel("#releases") {
        |                     id.set("C1234567890")
        |                 }
        |             }
        |         }
        |     }
        |     
        |     // Or for common bot configuration
        |     common {
        |         bots {
        |             bot("commonBot") {
        |                 token.set(providers.gradleProperty("slack.token"))
        |                 // ...
        |             }
        |         }
        |     }
        | }
        |
        | NOTE: The bot token should be stored securely and not committed to version control.
        | Consider using Gradle properties or environment variables.
        |============================================================
    """.trimMargin()
}

fun blockTextHasMoreSymbolsMessage(maxSymbols: Int): String {
    return """
        |
        |============================================================
        |                   ⚠️ MESSAGE TOO LONG ⚠️
        |============================================================
        | The message block text exceeds the maximum allowed length
        | of $maxSymbols characters.
        |
        | RECOMMENDED ACTIONS:
        | 1. Split the message into multiple blocks
        | 2. Reduce the amount of text in the message
        | 3. Consider using a file upload for large content
        |
        | Current length: [Not calculated - message was truncated]
        | Maximum allowed: $maxSymbols characters
        |============================================================
    """.trimMargin()
}

fun headerTextHasMoreSymbolsMessage(maxSymbols: Int): String {
    return """
        |
        |============================================================
        |                ⚠️ HEADER TEXT TOO LONG ⚠️
        |============================================================
        | The header text exceeds the maximum allowed length
        | of $maxSymbols characters.
        |
        | RECOMMENDED ACTIONS:
        | 1. Make the header more concise
        | 2. Move detailed information to the message body
        |
        | Current length: [Not calculated - header was truncated]
        | Maximum allowed: $maxSymbols characters
        |============================================================
    """.trimMargin()
}

fun failedToSendChangelogMessage(webhookUrl: String): String {
    return """
        |
        |============================================================
        |          ❌ FAILED TO SEND CHANGELOG TO SLACK ❌
        |============================================================
        | Target Webhook: ${webhookUrl.take(50)} (trunked)...
        |
        | POSSIBLE CAUSES:
        | 1. Invalid or expired webhook URL
        | 2. Network connectivity issues
        | 3. Slack API rate limiting
        | 4. Invalid message format
        |
        | TROUBLESHOOTING STEPS:
        | 1. Verify the webhook URL is correct and active
        | 2. Check your network connection
        | 3. Try again after a short delay
        | 4. Check the Slack API status page for outages
        |
        | For more information, check the full error in the logs.
        |============================================================
    """.trimMargin()
}
