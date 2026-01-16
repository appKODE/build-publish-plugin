package ru.kode.android.build.publish.plugin.slack.messages

import ru.kode.android.build.publish.plugin.slack.EXTENSION_NAME

fun uploadApiTokenRequiredMessage(): String {
    return """
        
        |============================================================
        |                 SLACK CONFIGURATION ERROR   
        |============================================================
        | The Slack plugin requires an upload API token file to be provided.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | ${EXTENSION_NAME} {
        |     bot {
        |         common {
        |             uploadApiTokenFile.set(File("path/to/your/token/file"))
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}

fun servicesCreatedMessages(servicesNames: Set<String>): String {
    return """
        
        |============================================================
        |          SLACK SERVICES INITIALIZED SUCCESSFULLY   
        |============================================================
        | The following Slack services have been created and configured:
        |
        | ${servicesNames.joinToString("\n") { "| • $it" }}
        |
        | These services will be used for sending notifications and
        | managing communications with Slack workspaces.
        |============================================================
        """.trimIndent()
}

fun registeringServicesMessage(): String {
    return """
        
        |============================================================
        |               REGISTERING SLACK SERVICES...   
        |============================================================
        | Initializing and configuring Slack services...
        | This may take a moment as we set up the necessary
        | connections and configurations.
        |============================================================
        """.trimIndent()
}

fun noBotsConfiguredMessage(): String {
    return """
        
        |============================================================
        |                NO SLACK BOTS CONFIGURED   
        |============================================================
        | No Slack bots have been configured in your build script.
        | The service map will remain empty.
        |
        | TO ENABLE SLACK NOTIFICATIONS:
        |  1. Add a 'slack' configuration block to your build script
        |  2. Define at least one bot with its API token and channels
        |
        | Example:
        | 
        | $EXTENSION_NAME {
        |     bots {
        |         common {
        |             bot("myBot") {
        |                 // Your bot settings here
        |             }
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}

fun mustApplyFoundationPluginMessage(): String {
    return """
        
        |============================================================
        |                 PLUGIN CONFIGURATION ERROR   
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
        | Make sure to replace <version> with the correct version 
        | number.
        |============================================================
        """.trimIndent()
}

fun extensionNotCreatedMessage(): String {
    return """
        
        |============================================================
        |                SLACK EXTENSION INITIALIZED   
        |============================================================
        | SlackServiceExtension has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        |  1. Configure your Slack settings in the build script
        |  2. Define your bots and channels
        |  3. Apply the configuration using the 'slack' extension
        |
        | Example configuration:
        | 
        | $EXTENSION_NAME {
        |     bots { ... }
        |     distribution { ... }
        |     changelog { ... }
        | }
        |============================================================
        """.trimIndent()
}

fun bundleDistributionNotCreatedMessage(): String {
    return """
        
        |============================================================
        |                BUNDLE DISTRIBUTION SKIPPED   
        |============================================================
        | The Slack distribution task for Android App Bundles was not
        | created because required configuration is missing.
        |
        | Add the following to your build script:
        |
        | $EXTENSION_NAME {
        |     distribution {
        |         common {
        |             destinationChannel("#releases")
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}

fun apkDistributionNotCreatedMessage(): String {
    return """
        
        |============================================================
        |                 APK DISTRIBUTION SKIPPED   
        |============================================================
        | The Slack distribution task for APK files was not created
        | because required configuration is missing.
        |
        | Add the following to your build script:
        |
        | $EXTENSION_NAME {
        |     distribution {
        |         common {
        |             destinationChannel("#releases")
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}

fun uploadFailedMessage(): String {
    return """
        
        |============================================================
        |                   SLACK UPLOAD TIMEOUT   
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
        |  1. Check your Slack channel to verify if the file was 
        |     uploaded
        |  2. If the file is present, you can safely ignore this 
        |     warning
        |  3. If the file is missing, try uploading again
        |============================================================
        """.trimIndent()
}

fun changelogFileNotFoundMessage(): String {
    return """
        
        |============================================================
        |             CHANGELOG FILE NOT FOUND OR EMPTY   
        |============================================================
        | The changelog file could not be found or is empty.
        |
        | POSSIBLE CAUSES:
        |  1. The changelog file was not generated
        |  2. The file path is incorrect
        |  3. The file exists but is empty
        |  4. Insufficient permissions to read the file
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the changelog file exists at the expected 
        |     location
        |  2. Check file permissions
        |  3. Ensure the changelog generation task ran successfully
        |  4. Verify the file is not empty
        |
        | If this is expected, you can ignore this message.
        |============================================================
        """.trimIndent()
}

fun changelogSentMessage(): String {
    return """
        
        |============================================================
        |            CHANGELOG SENT TO SLACK SUCCESSFULLY   
        |============================================================
        | The changelog has been successfully sent to the configured
        | Slack channel(s).
        |
        | NEXT STEPS:
        |  1. Check your Slack channel to verify the changelog
        |  2. Consider pinning the message for better visibility
        |
        | If you don't see the message, please check:
        | - The bot has permission to post in the channel
        | - The channel ID is correct
        | - The message isn't marked as spam by Slack
        |============================================================
        """.trimIndent()
}

fun provideChangelogOrDistributionConfigMessage(buildVariant: String): String {
    return """
        
        |============================================================
        |               MISSING REQUIRED CONFIGURATION   
        |============================================================
        | No valid configuration found for build variant: 
        | $buildVariant
        |
        | You must provide at least one of the following configurations:
        |  1. A changelog configuration
        |  2. A distribution configuration
        |
        | EXAMPLE CONFIGURATION:
        | $EXTENSION_NAME {
        |     common { // Or buildVariant($buildVariant)
        |         changelog { ... }
        |         // OR
        |         distribution { ... }
        |     }
        | }
        |
        | For more details, check the plugin documentation.
        |============================================================
        """.trimIndent()
}

fun provideBotConfigMessage(buildVariant: String): String {
    return """
        
        |============================================================
        |                 MISSING BOT CONFIGURATION   
        |============================================================
        | No bot configuration found for build variant: 
        | $buildVariant
        |
        | You must configure at least one bot in your build script.
        |
        | EXAMPLE CONFIGURATION:
        | 
        | $EXTENSION_NAME {
        |     common { // Or buildVariant($buildVariant)
        |         bots {
        |             bot("commonBot") {
        |                 // Bot configuration here
        |             }
        |         }
        |     }
        | }
        |
        | NOTE: 
        |  1. The bot token should be stored securely and not committed 
        |     to version control.
        |  2. Consider using Gradle properties or environment variables.
        |============================================================
        """.trimIndent()
}

fun blockTextHasMoreSymbolsMessage(maxSymbols: Int): String {
    return """
        
        |============================================================
        |                     MESSAGE TOO LONG   
        |============================================================
        | The message block text exceeds the maximum allowed length
        | of $maxSymbols characters.
        |
        | RECOMMENDED ACTIONS:
        |  1. Split the message into multiple blocks
        |  2. Reduce the amount of text in the message
        |  3. Consider using a file upload for large content
        |
        | Maximum allowed: $maxSymbols characters
        |============================================================
        """.trimIndent()
}

fun headerTextHasMoreSymbolsMessage(maxSymbols: Int): String {
    return """
        
        |============================================================
        |                   HEADER TEXT TOO LONG   
        |============================================================
        | The header text exceeds the maximum allowed length
        | of $maxSymbols characters.
        |
        | RECOMMENDED ACTIONS:
        |  1. Make the header more concise
        |  2. Move detailed information to the message body
        |
        | Maximum allowed: $maxSymbols characters
        |============================================================
        """.trimIndent()
}

fun failedToSendChangelogMessage(webhookUrl: String): String {
    return """
        |
        |============================================================
        |             FAILED TO SEND CHANGELOG TO SLACK   
        |============================================================
        | Target Webhook: ${webhookUrl.take(50)} (trunked)...
        |
        | POSSIBLE CAUSES:
        |  1. Invalid or expired webhook URL
        |  2. Network connectivity issues
        |  3. Slack API rate limiting
        |  4. Invalid message format
        |
        | TROUBLESHOOTING STEPS:
        |  1. Verify the webhook URL is correct and active
        |  2. Check your network connection
        |  3. Try again after a short delay
        |  4. Check the Slack API status page for outages
        |
        | For more information, check the full error in the logs.
        |============================================================
        """.trimIndent()
}
