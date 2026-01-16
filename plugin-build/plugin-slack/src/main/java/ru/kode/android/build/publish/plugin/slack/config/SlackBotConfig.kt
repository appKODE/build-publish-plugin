package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

/**
 * Configuration interface for a Slack bot that sends notifications.
 *
 * This interface defines the essential configuration needed to interact with a Slack bot
 * for sending build notifications. It's typically used in the build script's `slack` extension
 * to configure bot settings for different build variants.
 */
interface SlackBotConfig {
    /**
     * Name of this bot configuration.
     *
     * Used to match a configuration to a build variant (or to the common configuration).
     */
    val name: String

    /**
     * The Slack incoming webhook URL used to post messages.
     *
     * This URL is provided by Slack when you create an incoming webhook for your workspace.
     * It typically follows this format:
     * `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX`
     *
     * To set up a webhook:
     * 1. Go to https://api.slack.com/apps
     * 2. Create a new app or select an existing one
     * 3. Navigate to "Incoming Webhooks"
     * 4. Activate and create a new webhook
     * 5. Copy the Webhook URL
     *
     * @see <a href="https://api.slack.com/messaging/webhooks">Slack Incoming Webhooks</a>
     */
    @get:Input
    val webhookUrl: Property<String>

    /**
     * File containing the Slack API token with `files:write` scope.
     *
     * The token should have the following OAuth scopes:
     * - `files:write` - To upload files
     * - `channels:read` - To verify channel access
     * - `groups:read` - For private channel access
     *
     * @see <a href="https://api.slack.com/authentication/token-types#bot">Slack Bot Tokens</a>
     */
    @get:InputFile
    @get:Optional
    val uploadApiTokenFile: RegularFileProperty

    /**
     * The URL of an image to use as the bot's icon for messages.
     *
     * This can be any publicly accessible image URL. The image will be displayed
     * next to the bot's name in Slack messages.
     *
     * Recommended size: 512x512 pixels (will be resized by Slack)
     * Supported formats: PNG, JPEG, GIF (not animated), WebP
     *
     * Example: `https://example.com/images/bot-icon.png`
     *
     * If not provided, Slack will use the default app icon.
     */
    @get:Input
    val iconUrl: Property<String>
}
