@file:Suppress("FunctionOnlyReturningConstant") // Simple string providers

package ru.kode.android.build.publish.plugin.telegram.messages

import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalized
import ru.kode.android.build.publish.plugin.telegram.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.telegram.SERVICE_EXTENSION_NAME
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import java.io.File

fun sendingTelegramMessageMessage(): String = "Sending Telegram message"

fun uploadingTelegramFileMessage(): String = "Uploading file to Telegram"

fun configErrorMessage(chatName: String): String {
    return """
        |
        |============================================================
        |               TELEGRAM CONFIGURATION ERROR   
        |============================================================
        | Failed to get last message from Telegram chat
        |
        | Chat name: $chatName
        |
        | REQUIRED ACTIONS:
        |  1. Send ANY message from YOUR account to the 
        |     chat/channel/topic.
        |  2. Restart the task
        |============================================================
        """.trimIndent()
}

fun lookupSuccessMessage(
    botName: String,
    message: TelegramLastMessage,
): String {
    return """

        |============================================================
        |              TELEGRAM LAST MESSAGE DETECTED     
        |------------------------------------------------------------
        | Chat name : ${message.chatName}
        | Chat ID   : ${message.chatId}
        | Topic     : ${message.topicName ?: "—"}
        | Topic ID  : ${message.topicId ?: "—"}
        |------------------------------------------------------------
        | Message:
        | ${message.text ?: "—"}
        |------------------------------------------------------------
        | To apply these values in the build script, 
        | you need to add the following lines:
        |
        | $EXTENSION_NAME {
        |    bots {
        |       common {
        |           bot("$botName") {
        |               // Your bot settings here
        |           }
        |       }
        |    }
        | }
        |============================================================
        """.trimIndent()
}

fun mustApplyFoundationPluginMessage(): String {
    return """
        
        |============================================================
        |                 PLUGIN CONFIGURATION ERROR   
        |============================================================
        | The Telegram plugin requires the BuildPublishFoundationPlugin 
        | to be applied first.
        |
        | REQUIRED ACTION:
        |   Add the following to your module's build.gradle.kts file:
        |
        |   plugins {
        |       id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        |   }
        |
        | Make sure to replace <version> with the correct version 
        | number.
        |============================================================
        """.trimIndent()
}

fun configErrorExceptionMessage(chatName: String): String {
    return """
        
        |============================================================
        |               TELEGRAM CONFIGURATION ERROR   
        |============================================================
        |
        | Failed to get last message from Telegram chat: $chatName
        |
        | See instructions above in the build log.
        """.trimIndent()
}

fun telegramUploadFailedMessage(): String {
    return """
        
        |============================================================
        |                  TELEGRAM UPLOAD TIMEOUT   
        |============================================================
        | The file upload to Telegram timed out, but it might have 
        | been successful. 
        | This can happen due to network latency or server delays.
        |
        | RECOMMENDED ACTIONS:
        |  1. Check your Telegram chat to verify if the file was 
        |     uploaded
        |  2. If the file is present, you can safely ignore this
        |     warning
        |  3. If the file is missing, try uploading again
        |
        | If the issue persists, check your network connection or
        | Telegram API status.
        |============================================================
        """.trimIndent()
}

fun failedToReadChangelogFile(file: File?): String {
    return if (file != null) {
        """
        
        |============================================================
        |                   CHANGELOG FILE ERROR   
        |============================================================
        | The changelog file at:
        |   ${file.absolutePath}
        | exists but is empty or could not be read.
        |
        | POSSIBLE CAUSES:
        |  1. The file is empty (0 bytes)
        |  2. Insufficient permissions to read the file
        |  3. The file is locked by another process
        |
        | RECOMMENDED ACTIONS:
        |  1. Check if the file exists and has content
        |  2. Verify file permissions
        |  3. Ensure no other process is locking the file
        """.trimIndent()
    } else {
        """
            
            |============================================================
            |                   CHANGELOG FILE ERROR   
            |============================================================
            | The changelog file does not exist at the specified path.
            |
            | POSSIBLE CAUSES:
            |  1. The changelog file was not generated
            |  3. The changelog generation task was not executed
            |
            | RECOMMENDED ACTIONS:
            |  1. Check if the changelog file exists in the expected 
            |    location
            |  2. Run the changelog generation task before this step
            """
    }.trimIndent()
}

fun changelogSentMessage(): String {
    return """
        
        |============================================================
        |              CHANGELOG UPLOADED SUCCESSFULLY   
        |============================================================
        | The changelog has been successfully sent to the configured
        | Telegram chat(s).
        |
        | NEXT STEPS:
        |  1. Check your Telegram chat to verify the changelog
        |  2. Consider pinning the message for better visibility
        |
        | If you don't see the message, please check:
        | - The bot has permission to post in the chat
        | - The chat ID and topic ID (if used) are correct
        | - The message isn't marked as spam by Telegram
        |============================================================
        """.trimIndent()
}

fun noMatchingConfigurationMessage(botName: String): String {
    return """
        
        |============================================================
        |                 BOT CONFIGURATION MISSING   
        |============================================================
        | No configuration found for bot: $botName
        |
        | REQUIRED ACTION:
        | Add the bot configuration to your build script:
        |
        | $EXTENSION_NAME {
        |     bots {
        |         common {
        |             bot("$botName") {
        |                 // Your bot settings here
        |             }
        |         }
        |     }
        | }
        |
        | After updating, sync your project and try again.
        |============================================================
        """.trimIndent()
}

fun noMatchingConfigurationMessage(destinationBot: DestinationTelegramBot): String {
    return """
        
        |============================================================
        |              BOT/CHAT CONFIGURATION MISMATCH   
        |============================================================
        | Could not find matching configuration for:
        | - Bot Name: ${destinationBot.botName}
        | - Chat Names: ${destinationBot.chatNames}
        |
        | POSSIBLE ISSUES:
        |  1. The bot '${destinationBot.botName}' is not configured
        |  2. The chat names don't match any configured chat
        |  3. The configuration is in the wrong scope (common/variant-specific)
        |
        | REQUIRED ACTIONS:
        |  1. Verify the bot name matches exactly (case-sensitive):
        |    - Check for typos in '${destinationBot.botName}'
        |
        |  2. Check your chat names match exactly:
        |    - Configured: ${destinationBot.chatNames}
        |    - Should match the 'chat' block names in your bot config
        |
        |  3. Example configuration:
        |
        |    $EXTENSION_NAME {
        |        bots {
        |            common {  // Or specific variant
        |                bot("${destinationBot.botName}") {
        |                    // Your bot settings here
        |                }
        |            }
        |        }
        |    }
        |
        | 4. After fixing, sync your project and rebuild.
        |============================================================
        """.trimIndent()
}

fun needToProvideChangelogOrDistributionConfigMessage(buildVariant: String): String {
    return """
        
        |============================================================
        |                   MISSING CONFIGURATION   
        |============================================================
        | The Telegram plugin requires either a changelog or distribution
        | configuration for the '$buildVariant' build variant.
        |
        | REQUIRED CONFIGURATION:
        | You need to configure at least one of these in your build script:
        |
        | 1. Changelog Configuration (recommended for release notes):
        |    $EXTENSION_NAME {
        |        changelog {
        |            common { // Or buildVariant("$buildVariant")
        |                // Your changelog settings here
        |            }
        |        }
        |    }
        |
        | 2. Distribution Configuration (for file uploads):
        |    $EXTENSION_NAME {
        |        distribution {
        |            common { // Or buildVariant("$buildVariant")
        |                // Your distribution settings here
        |            }
        |        }
        |    }
        |
        | LOOKUP INSTRUCTIONS:
        | If you don't know your chatId or topicId, you can find them using:
        |    ./gradlew ${TaskNames.Telegram.LOOKUP_PREFIX}${buildVariant.capitalized()}
        |
        | After running the lookup task, use the provided configuration
        | snippet to update your build script.
        |============================================================
        """.trimIndent()
}

fun needToProvideBotsConfigMessage(buildVariant: String): String {
    return """
        
        |============================================================
        |                MISSING BOTS CONFIGURATION   
        |============================================================
        | The Telegram plugin requires a 'bots' configuration block
        | for the '$buildVariant' build variant or 'common'.
        |
        | REQUIRED CONFIGURATION:
        | Add the following to your build script:
        |
        |    $EXTENSION_NAME {
        |        bots {
        |            common { // Or buildVariant("$buildVariant")
        |                // Your bot settings here
        |            }
        |        }
        |    }
        |
        | GETTING STARTED:
        |  1. Create a bot using @BotFather on Telegram
        |  2. Add the bot to your chat/channel
        |  3. Get the chat ID (and topic ID if using topics)
        |  4. Fill in the configuration above
        |
        | LOOKUP INSTRUCTIONS:
        | You can find chatId and topicId using the lookup task:
        |    ./gradlew ${TaskNames.Telegram.LOOKUP_PREFIX}${buildVariant.capitalized()}
        |
        | After configuration, sync your project and try again.
        |============================================================
        """.trimIndent()
}

fun telegramServicesCreated(servicesKeys: Set<String>): String {
    return """
        
        |============================================================
        |               TELEGRAM SERVICES INITIALIZED   
        |============================================================
        | The following Telegram services have been created:
        |
        | ${servicesKeys.joinToString("\n| ")}
        |
        | These services will handle communication with the
        | Telegram Bot API for various operations.
        |============================================================
        """.trimIndent()
}

fun registeringServiceMessage(): String {
    return """
        
        |============================================================
        |               REGISTERING TELEGRAM SERVICES   
        |============================================================
        | Initializing and registering Telegram plugin services...
        |
        | This includes:
        | - Bot API clients
        | - Message processors
        | - File upload handlers
        |
        | Please wait while services are being configured.
        |============================================================
        """.trimIndent()
}

fun noBotsConfiguredMessage(): String {
    return """
        
        |============================================================
        |                NO TELEGRAM BOTS CONFIGURED   
        |============================================================
        | The Telegram plugin is active but no bots are configured.
        |
        | NEXT STEPS:
        |  1. Add bot configurations to your build script:
        |
        |    $EXTENSION_NAME {
        |        bots {
        |            common { // Or buildVariant("<BUILD_VARIANT>")
        |                // Your bot configuration
        |            }
        |        }
        |    }
        |
        |  2. Sync your project after making changes
        |
        | The plugin will continue to run but won't send any messages
        | until at least one bot is properly configured.
        |============================================================
        """.trimIndent()
}

fun extensionCreatedMessage(): String {
    return """
        
        |============================================================
        |               TELEGRAM EXTENSION INITIALIZED    
        |============================================================
        | The $SERVICE_EXTENSION_NAME has been created.
        |
        | STATUS: Empty configuration detected
        |
        | NEXT STEPS:
        |  1. Configure the extension in your build script:
        |
        |    $EXTENSION_NAME {
        |        // Your configuration here
        |    }
        |
        |  2. Sync your project to apply changes
        |
        | The extension is now ready to be configured with your
        | Telegram bot and chat settings.
        |============================================================
        """.trimIndent()
}

fun distributionBundleTaskNotCreatedMessage(): String {
    return """
        
        |============================================================
        |              BUNDLE DISTRIBUTION TASK SKIPPED   
        |============================================================
        | The Telegram distribution task for Android App Bundles (AAB)
        | was not created because no destination bots are configured.
        |
        | REASON:
        | The 'destinationBots' collection is empty or not configured
        | for the current build variant.
        |
        | TO ENABLE BUNDLE DISTRIBUTION:
        |  1. Add at least one destination bot configuration:
        |
        |    $EXTENSION_NAME {
        |        distribution {
        |            common { // Or buildVariant("<BUILD_VARIANT>")
        |                // Your distribution settings here
        |            }
        |        }
        |    }
        |
        |  2. Ensure your build variant matches the configuration or 
        |     use common
        |  3. Sync your project and rebuild
        |
        | The task will be automatically created when valid
        | configurations are provided.
        |============================================================
        """.trimIndent()
}

fun distributionTaskNotCreatedMessage(): String {
    return """
        
        |============================================================
        |              APK DISTRIBUTION TASK SKIPPED  
        |============================================================
        | The Telegram distribution task for APK files was not created
        | because no destination bots are configured.
        |
        | REASON:
        | The 'destinationBots' collection is empty or not configured
        | for the current build variant.
        |
        | TO ENABLE APK DISTRIBUTION:
        |  1. Add at least one destination bot configuration:
        |
        |    $EXTENSION_NAME {
        |        distribution {
        |            common { // Or buildVariant("<BUILD_VARIANT>")
        |                // Your distribution settings here
        |            }
        |        }
        |    }
        |
        |  2. Ensure your build variant matches the configuration or 
        |     use common
        |  3. Sync your project and rebuild
        |
        | The task will be automatically created when valid
        | configurations are provided.
        |============================================================
        """.trimIndent()
}
