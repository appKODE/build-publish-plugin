package ru.kode.android.build.publish.plugin.telegram.service

import okhttp3.OkHttpClient
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramController
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerImpl
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.telegramBotFromJson
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory
import java.io.File
import javax.inject.Inject

private const val TELEGRAM_BASE_RUL = "https://api.telegram.org"

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
     * Configuration parameters for the TelegramNetworkService.
     */
    interface Params : BuildServiceParameters {
        /**
         * A list of configured Telegram bots that can be used for sending messages.
         * Each bot is identified by its name and contains the necessary credentials.
         */
        val bots: ListProperty<String>
    }

    internal abstract val okHttpClientProperty: Property<OkHttpClient>
    internal abstract val retrofitBuilderProperty: Property<Retrofit.Builder>
    internal abstract val distributionApiProperty: Property<TelegramDistributionApi>
    internal abstract val webhookApiProperty: Property<TelegramWebhookApi>
    internal abstract val controllerProperty: Property<TelegramController>

    init {
        okHttpClientProperty.set(
            TelegramClientFactory.build()
        )
        retrofitBuilderProperty.set(
            okHttpClientProperty.map { client ->
                TelegramRetrofitBuilderFactory.build(client)
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
            webhookApiProperty.zip(distributionApiProperty) { a, b ->
                TelegramControllerImpl(a, b)
            }
        )
    }

    private val bots: List<TelegramBot> get() = parameters.bots
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
        userMentions: List<String>?,
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
                fallbackServerBaseUrl = TELEGRAM_BASE_RUL
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
                fallbackServerBaseUrl = TELEGRAM_BASE_RUL
            ),
        )
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
private fun Collection<TelegramBot>.mapToChatSpecificBots(
    destinationBots: List<DestinationTelegramBot>,
    fallbackServerBaseUrl: String
): List<ChatSpecificTelegramBot> {
    return destinationBots
        .flatMap { destinationBot ->
            val botName = destinationBot.botName
            val chatNames = destinationBot.chatNames
            val bot = this.firstOrNull { it.name == botName }
            bot?.chats?.filter { it.name in chatNames }
                ?.map { chat ->
                    val authPassword = bot.basicAuth?.password
                    val authUserName = bot.basicAuth?.username
                    val basicAuth =
                        if (authUserName != null && authPassword != null) {
                            ChatSpecificTelegramBot.BasicAuth(authUserName, authPassword)
                        } else {
                            null
                        }
                    ChatSpecificTelegramBot(
                        name = bot.name,
                        id = bot.id,
                        serverBaseUrl = bot.serverBaseUrl ?: fallbackServerBaseUrl,
                        basicAuth = basicAuth,
                        chatId = chat.id,
                        topicId = chat.topicId,
                    )
                } ?: throw GradleException(
                "No matching bot configuration found for botName: ${destinationBot.botName} and chatNames: ${destinationBot.chatNames}.\n" +
                        "Make sure that the botName and chatNames in the destinationBot configuration match a botName and chatNames in the bots configuration.\n" +
                        "To fix this, update the destinationBot configuration to match a botName and chatNames in the bots configuration."
                )
        }
}
