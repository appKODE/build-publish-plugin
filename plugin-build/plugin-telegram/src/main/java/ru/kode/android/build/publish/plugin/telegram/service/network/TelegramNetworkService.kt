package ru.kode.android.build.publish.plugin.telegram.service.network

import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.telegram.config.DestinationBot
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.task.changelog.api.TelegramWebhookSenderApi
import ru.kode.android.build.publish.plugin.telegram.task.distribution.api.TelegramDistributionApi
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val STUB_BASE_URL = "http://localhost/"
private const val TELEGRAM_BASE_RUL = "https://api.telegram.org"

private const val SEND_MESSAGE_TO_CHAT_WEB_HOOK =
    "%s/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2&disable_web_page_preview=true"
private const val SEND_MESSAGE_TO_TOPIC_WEB_HOOK =
    "%s/bot%s/sendMessage?chat_id=%s&message_thread_id=%s&text=%s&parse_mode=MarkdownV2" +
        "&disable_web_page_preview=true"
private const val SEND_DOCUMENT_WEB_HOOK = "https://%s/bot%s/sendDocument"

private val logger: Logger = Logging.getLogger(TelegramNetworkService::class.java)

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
abstract class TelegramNetworkService
    @Inject
    constructor() : BuildService<TelegramNetworkService.Params> {
        /**
         * Configuration parameters for the TelegramNetworkService.
         */
        interface Params : BuildServiceParameters {
            /**
             * A list of configured Telegram bots that can be used for sending messages.
             * Each bot is identified by its name and contains the necessary credentials.
             */
            val bots: Property<List<TelegramBotConfig>>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val retrofitProperty: Property<Retrofit.Builder>
        internal abstract val distributionApiProperty: Property<TelegramDistributionApi>
        internal abstract val senderApiProperty: Property<TelegramWebhookSenderApi>

        init {
            okHttpClientProperty.set(
                OkHttpClient.Builder()
                    .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .addProxyIfAvailable()
                    .apply {
                        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                        addNetworkInterceptor(loggingInterceptor)
                    }
                    .build(),
            )
            retrofitProperty.set(
                okHttpClientProperty.map { client ->
                    val moshi = Moshi.Builder().build()
                    Retrofit.Builder()
                        .baseUrl(STUB_BASE_URL)
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                },
            )
            distributionApiProperty.set(
                retrofitProperty.map { retrofit ->
                    retrofit.build().create(TelegramDistributionApi::class.java)
                },
            )
            senderApiProperty.set(
                retrofitProperty.map { retrofit ->
                    retrofit.build().create(TelegramWebhookSenderApi::class.java)
                },
            )
        }

        private val distributionApi: TelegramDistributionApi get() = distributionApiProperty.get()
        private val senderApi: TelegramWebhookSenderApi get() = senderApiProperty.get()
        private val bots: List<TelegramBotConfig> get() = parameters.bots.get()

        /**
         * Sends a text message to the specified Telegram chats using the configured bots.
         *
         * This method sends a Markdown-formatted message to one or more Telegram chats
         * using the specified bots. The message will be properly escaped for Telegram's
         * MarkdownV2 format.
         *
         * @param message The message to send (supports MarkdownV2 formatting)
         * @param destinationBots Set of destination bots and their respective chat configurations
         *
         * @throws IllegalStateException If no matching bot configuration is found
         * @throws IOException If there's a network error while sending the message
         *
         * @see upload For sending files instead of text messages
         */
        fun send(
            message: String,
            destinationBots: Set<DestinationBot>,
        ) {
            bots.getBy(destinationBots)
                .forEach { bot ->
                    val topicId = bot.topicId
                    val webhookUrl =
                        if (topicId.isNullOrEmpty()) {
                            SEND_MESSAGE_TO_CHAT_WEB_HOOK.format(
                                bot.serverBaseUrl,
                                bot.id,
                                bot.chatId,
                                URLEncoder.encode(message, "utf-8"),
                            )
                        } else {
                            SEND_MESSAGE_TO_TOPIC_WEB_HOOK.format(
                                bot.serverBaseUrl,
                                bot.id,
                                bot.chatId,
                                topicId,
                                URLEncoder.encode(message, "utf-8"),
                            )
                        }
                    logger.info("sending changelog to ${bot.name} by $webhookUrl")
                    val authorization =
                        bot.basicAuth
                            ?.let { Credentials.basic(it.username, it.password) }
                    senderApi
                        .send(authorization, webhookUrl)
                        .executeNoResult()
                }
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
            destinationBots: Set<DestinationBot>,
        ) {
            bots.getBy(destinationBots)
                .forEach { bot ->
                    val webhookUrl = SEND_DOCUMENT_WEB_HOOK.format(bot.serverBaseUrl, bot.id)
                    val filePart =
                        MultipartBody.Part.createFormData(
                            "document",
                            file.name,
                            file.asRequestBody(),
                        )
                    val topicId = bot.topicId
                    val params =
                        if (topicId != null) {
                            hashMapOf(
                                "message_thread_id" to createPartFromString(topicId),
                                "chat_id" to createPartFromString(bot.chatId),
                            )
                        } else {
                            hashMapOf(
                                "chat_id" to createPartFromString(bot.chatId),
                            )
                        }
                    logger.info("upload file to ${bot.name} by $webhookUrl")
                    val authorization =
                        bot.basicAuth
                            ?.let { Credentials.basic(it.username, it.password) }
                    distributionApi
                        .upload(authorization, webhookUrl, params, filePart)
                        .executeWithResult()
                        .getOrThrow()
                }
        }
    }

/**
 * Retrieves a list of [TelegramBot] configurations based on the provided [destinationBots].
 *
 * This function filters the list of [TelegramBotConfig] based on the matching [botName] and [chatNames].
 * It then maps each matching [TelegramChatConfig] to a [TelegramBot] instance,
 * using the corresponding [botName], [botId], [botServerBaseUrl], [basicAuth], [chatId], and [topicId].
 *
 * @param destinationBots The set of [DestinationBot] configurations.
 *
 * @return A list of [TelegramBot] configurations that match the provided [destinationBots].
 */
private fun List<TelegramBotConfig>.getBy(destinationBots: Set<DestinationBot>): List<TelegramBot> {
    return destinationBots
        .flatMap { destinationBot ->
            val botName = destinationBot.botName.get()
            val chatNames = destinationBot.chatNames.get()
            val bot = this.firstOrNull { it.name == botName }
            bot?.chats?.filter { it.name in chatNames }
                ?.map { chat ->
                    val authPassword = bot.botServerAuth.password.orNull
                    val authUserName = bot.botServerAuth.username.orNull
                    val basicAuth =
                        if (authUserName != null && authPassword != null) {
                            TelegramBot.BasicAuth(authUserName, authPassword)
                        } else {
                            null
                        }
                    TelegramBot(
                        name = bot.name,
                        id = bot.botId.get(),
                        serverBaseUrl = bot.botServerBaseUrl.orNull ?: TELEGRAM_BASE_RUL,
                        basicAuth = basicAuth,
                        chatId = chat.chatId.get(),
                        topicId = chat.topicId.orNull,
                    )
                }
                .orEmpty()
        }
}

/**
 * Represents a Telegram bot used for sending messages.
 */
private data class TelegramBot(
    /**
     * The name of this bot configuration (automatically set from the registration name)
     */
    val name: String,
    /**
     * The bot token (e.g., "1234567890:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
     */
    val id: String,
    /**
     * The base URL for the Telegram Bot API server (defaults to "https://api.telegram.org")
     */
    val serverBaseUrl: String,
    /**
     * The unique identifier of the chat where the bot will send messages
     */
    val chatId: String,
    /**
     * Optional thread identifier for forum topics or group threads
     */
    val topicId: String?,
    /**
     * Optional basic authentication credentials for the Bot API server
     */
    val basicAuth: BasicAuth?,
) {
    /**
     * Represents basic authentication credentials for the Telegram Bot API server.
     */
    data class BasicAuth(
        /**
         * The username for basic authentication
         */
        val username: String,
        /**
         * The password for basic authentication
         */
        val password: String,
    )
}
