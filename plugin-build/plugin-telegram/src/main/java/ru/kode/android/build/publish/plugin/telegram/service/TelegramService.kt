package ru.kode.android.build.publish.plugin.telegram.service

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramController
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerImpl
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.telegramBotFromJson
import ru.kode.android.build.publish.plugin.telegram.messages.noMatchingConfigurationMessage
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory
import java.io.File
import javax.inject.Inject

/**
 * A Gradle [BuildService] that handles network communication with the Telegram Bot API.
 *
 * This service is responsible for sending messages and files to Telegram chats using the
 * Telegram Bot API. It supports both simple text messages and file uploads, with support
 * for topics and multiple chat destinations.
 *
 * The service is implemented as a Gradle Build Service to ensure proper resource cleanup
 * and to maintain a single HTTP client instance across builds.
 *
 * @see BuildService For more information about Gradle Build Services
 * @see Params For configuration parameters
 */
abstract class TelegramService
    @Inject
    constructor() : BuildService<TelegramService.Params> {
        /**
         * Configuration parameters for the TelegramService.
         */
        interface Params : BuildServiceParameters {
            /**
             * A list of configured Telegram bots that can be used for sending messages.
             * Each bot is identified by its name and contains the necessary credentials.
             */
            val bots: ListProperty<String>
        }

        private val logger: Logger = Logging.getLogger("Telegram")

        private val json = Json { ignoreUnknownKeys = true }

        internal abstract val clientProperty: Property<OkHttpClient>
        internal abstract val retrofitBuilderProperty: Property<Retrofit.Builder>
        internal abstract val distributionApiProperty: Property<TelegramDistributionApi>
        internal abstract val webhookApiProperty: Property<TelegramWebhookApi>
        internal abstract val controllerProperty: Property<TelegramController>

        init {
            clientProperty.set(
                TelegramClientFactory.build(logger, json),
            )
            retrofitBuilderProperty.set(
                clientProperty.map { client ->
                    TelegramRetrofitBuilderFactory.build(client, json)
                },
            )
            distributionApiProperty.set(
                retrofitBuilderProperty.map { retrofitBuilder ->
                    TelegramDistributionApiFactory.build(retrofitBuilder)
                },
            )
            webhookApiProperty.set(
                retrofitBuilderProperty.map { retrofitBuilder ->
                    TelegramWebhookApiFactory.build(retrofitBuilder)
                },
            )
            controllerProperty.set(
                webhookApiProperty.zip(distributionApiProperty) { webhookApi, distributionApi ->
                    TelegramControllerImpl(webhookApi, distributionApi, logger)
                },
            )
        }

        private val bots: List<TelegramBot>
            get() =
                parameters.bots
                    .map { it.map { telegramBotFromJson(it) } }
                    .get()

        private val controller: TelegramController get() = controllerProperty.get()

        /**
         * Sends a text message to the specified Telegram chats using the configured bots in chunks.
         *
         * This method sends a Markdown-formatted message to one or more Telegram chats
         * using the specified bots. The message will be split into chunks if it exceeds the maximum
         * length allowed by Telegram. The chunks will be separated by newlines.
         *
         * @param message The message to send (supports MarkdownV2 formatting)
         * @param bots List of bot configurations
         * @param destinationBots Set of destination bots and their respective chat configurations
         *
         * @throws IllegalStateException If no matching bot configuration is found
         * @throws IOException If there's a network error while sending the message
         *
         * @see upload For sending files instead of text messages
         */
        fun send(
            changelog: String,
            header: String,
            userMentions: List<String>,
            issueUrlPrefix: String,
            issueNumberPattern: String,
            destinationBots: List<DestinationTelegramBot>,
        ) {
            controller.send(
                changelog,
                header,
                userMentions,
                issueUrlPrefix,
                issueNumberPattern,
                bots.mapToChatSpecificBots(
                    destinationBots = destinationBots,
                ),
            )
        }

        /**
         * Uploads a file to the specified Telegram chats using the configured bots.
         *
         * This method sends a file to one or more Telegram chats using the specified bots.
         * The file will be sent as a document, and a caption can be included.
         *
         * @param file The file to upload
         * @param destinationBots Set of destination bots and their respective chat configurations
         *
         * @throws IllegalStateException If no matching bot configuration is found
         * @throws IOException If there's a network error or the file cannot be read
         *
         * @see send For sending text messages without file attachments
         */
        fun upload(
            file: File,
            destinationBots: List<DestinationTelegramBot>,
        ) {
            controller.upload(
                file,
                bots.mapToChatSpecificBots(
                    destinationBots = destinationBots,
                ),
            )
        }

        /**
         * Retrieves the last message sent to the specified chat.
         *
         * @param bot The ID of the bot to use for retrieving the message
         * @param chatName The ID of the chat to retrieve the last message from
         * @param topicName The ID of the topic to retrieve the last message from; if null, retrieves the last message from the chat
         *
         * @return The last message sent to the chat, or null if no message was found
         *
         * @throws IllegalStateException If no matching bot configuration is found
         * @throws IOException If there's a network error while retrieving the message
         */
        fun getLastMessage(
            botName: String,
            chatName: String,
            topicName: String?,
        ): TelegramLastMessage? {
            val bot =
                bots.firstOrNull { it.name == botName }
                    ?: throw GradleException(noMatchingConfigurationMessage(botName))
            return controller.getLastMessage(bot.id, chatName, topicName)
        }
    }

/**
 * Retrieves a list of [ChatSpecificTelegramBot] configurations based on the provided [destinationBots].
 *
 * This function filters the list of [TelegramBotConfig] based on the matching [botName] and [chatNames].
 * It then maps each matching [TelegramChatConfig] to a [ChatSpecificTelegramBot] instance,
 * using the corresponding [botName], [botId], [botServerBaseUrl], [basicAuth], [chatId], and [topicId].
 *
 * @param destinationBots The set of [DestinationTelegramBotConfig] configurations.
 *
 * @return A list of [ChatSpecificTelegramBot] configurations that match the provided [destinationBots].
 */
@Suppress("MaxLineLength")
private fun Collection<TelegramBot>.mapToChatSpecificBots(destinationBots: List<DestinationTelegramBot>): List<ChatSpecificTelegramBot> {
    return destinationBots
        .flatMap { destinationBot ->
            val botName = destinationBot.botName
            val bot = this.firstOrNull { it.name == botName }
            bot?.mapToChatSpecificBot(destinationBot)
                ?: throw GradleException(noMatchingConfigurationMessage(destinationBot))
        }
}

/**
 * Retrieves a list of [ChatSpecificTelegramBot] configurations based on the provided [destinationBot].
 *
 * This function filters the list of [TelegramChatConfig] based on the matching [chatNames].
 * It then maps each matching [TelegramChatConfig] to a [ChatSpecificTelegramBot] instance,
 * using the corresponding [name], [id], [serverBaseUrl], [basicAuth], [chatId], and [topicId].
 *
 * @param destinationBot The [DestinationTelegramBotConfig] configuration.
 * @param fallbackServerBaseUrl The fallback server base URL to use if [serverBaseUrl] is not set.
 *
 * @return A list of [ChatSpecificTelegramBot] configurations that match the provided [destinationBot].
 */
@Suppress("TooGenericExceptionThrown")
private fun TelegramBot.mapToChatSpecificBot(destinationBot: DestinationTelegramBot): List<ChatSpecificTelegramBot> {
    val chatNames = destinationBot.chatNames
    return chats.filter { it.name in chatNames }
        .map { chat ->
            val authPassword = this.basicAuth?.password
            val authUserName = this.basicAuth?.username
            val basicAuth =
                if (authUserName != null && authPassword != null) {
                    ChatSpecificTelegramBot.BasicAuth(authUserName, authPassword)
                } else {
                    null
                }
            ChatSpecificTelegramBot(
                name = this.name,
                id = this.id,
                serverBaseUrl = this.serverBaseUrl,
                basicAuth = basicAuth,
                chatId = chat.id,
                topicId = chat.topicId,
            )
        }
}
