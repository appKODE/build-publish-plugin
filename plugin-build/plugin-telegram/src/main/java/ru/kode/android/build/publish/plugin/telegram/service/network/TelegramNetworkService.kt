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
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
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
private const val TELEGRAM_BASE_RUL = "api.telegram.org"

private const val SEND_MESSAGE_TO_CHAT_WEB_HOOK =
    "https://%s/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2&disable_web_page_preview=true"
private const val SEND_MESSAGE_TO_TOPIC_WEB_HOOK =
    "https:/%s/bot%s/sendMessage?chat_id=%s&message_thread_id=%s&text=%s&parse_mode=MarkdownV2" +
        "&disable_web_page_preview=true"
private const val SEND_DOCUMENT_WEB_HOOK = "https://%s/bot%s/sendDocument"

abstract class TelegramNetworkService
    @Inject
    constructor() : BuildService<TelegramNetworkService.Params> {
        interface Params : BuildServiceParameters {
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
         * Sends url formatted data to webhook at [webhookUrl]
         */
        fun send(message: String, destinationBots: Set<DestinationBot>) {
            resolveBots(destinationBots)
                .forEach { bot ->
                    val topicId = bot.topicId
                    val webhookUrl =
                        if (topicId.isNullOrEmpty()) {
                            SEND_MESSAGE_TO_CHAT_WEB_HOOK.format(
                                bot.serverBaseUrl,
                                bot.it,
                                bot.chatId,
                                URLEncoder.encode(message, "utf-8"),
                            )
                        } else {
                            SEND_MESSAGE_TO_TOPIC_WEB_HOOK.format(
                                bot.serverBaseUrl,
                                bot.it,
                                bot. chatId,
                                topicId,
                                URLEncoder.encode(message, "utf-8"),
                            )
                        }
                    logger.info("sending changelog to $webhookUrl")
                    if (bot.basicAuth != null) {
                        senderApi
                            .sendAuthorised(
                                Credentials.basic(bot.basicAuth.username, bot.basicAuth.password),
                                webhookUrl
                            )
                            .executeOrThrow()
                    } else {
                        senderApi
                            .send(webhookUrl)
                            .executeOrThrow()
                    }
                }
        }

        fun upload(file: File, destinationBots: Set<DestinationBot>) {
            resolveBots(destinationBots)
                .forEach { bot ->
                    val webhookUrl = SEND_DOCUMENT_WEB_HOOK.format(bot.serverBaseUrl, bot.it)
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
                    if (bot.basicAuth != null) {
                        distributionApi
                            .uploadAuthorised(
                                Credentials.basic(bot.basicAuth.username, bot.basicAuth.password),
                                webhookUrl,
                                params,
                                filePart
                            )
                            .executeOrThrow()
                    } else {
                        distributionApi
                            .upload(webhookUrl, params, filePart)
                            .executeOrThrow()
                    }
                }
            }

            private fun resolveBots(destinationBots: Set<DestinationBot>): List<TelegramBot> {
                return destinationBots
                    .flatMap { destinationBot ->
                        val botName = destinationBot.botName.get()
                        val chatNames = destinationBot.chatNames.get()
                        val bot = bots.firstOrNull { it.name == botName }
                        bot?.chats?.filter { it.name in chatNames }
                            ?.map { chat ->
                                val authPassword = bot.botServerAuth.password.orNull
                                val authUserName = bot.botServerAuth.username.orNull
                                val basicAuth = if (authUserName != null && authPassword != null) {
                                    TelegramBot.BasicAuth(authUserName, authPassword)
                                } else {
                                    null
                                }
                                TelegramBot(
                                    it = bot.botId.get(),
                                    serverBaseUrl = bot.botServerBaseUrl.orNull ?: TELEGRAM_BASE_RUL,
                                    basicAuth = basicAuth,
                                    chatId = chat.chatId.get(),
                                    topicId = chat.topicId.orNull,
                                )
                            }.orEmpty()
                    }
            }

            companion object {
                private val logger: Logger = Logging.getLogger(TelegramNetworkService::class.java)
            }
    }

private data class TelegramBot(
    val it: String,
    val serverBaseUrl: String,
    val chatId: String,
    val topicId: String?,
    val basicAuth: BasicAuth?

) {
    data class BasicAuth(
        val username: String,
        val password: String
    )
}
