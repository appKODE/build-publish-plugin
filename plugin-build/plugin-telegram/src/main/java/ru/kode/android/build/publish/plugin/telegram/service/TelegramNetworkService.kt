package ru.kode.android.build.publish.plugin.telegram.service

import com.squareup.moshi.Moshi
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
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import ru.kode.android.build.publish.plugin.telegram.task.changelog.api.TelegramWebhookSenderApi
import ru.kode.android.build.publish.plugin.telegram.task.distribution.api.TelegramApi
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val STUB_BASE_URL = "http://localhost/"
private const val SEND_MESSAGE_TO_CHAT_WEB_HOOK =
    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2&disable_web_page_preview=true"
private const val SEND_MESSAGE_TO_TOPIC_WEB_HOOK =
    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&message_thread_id=%s&text=%s&parse_mode=MarkdownV2" +
        "&disable_web_page_preview=true"
private const val SEND_DOCUMENT_WEB_HOOK = "https://api.telegram.org/bot%s/sendDocument"

abstract class TelegramNetworkService
    @Inject
    constructor() : BuildService<TelegramNetworkService.Params> {
        interface Params : BuildServiceParameters {
            val botId: Property<String>
            val chatId: Property<String>
            val topicId: Property<String>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val retrofitProperty: Property<Retrofit.Builder>
        internal abstract val apiProperty: Property<TelegramApi>
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
            apiProperty.set(
                retrofitProperty.map { retrofit ->
                    retrofit.build().create(TelegramApi::class.java)
                },
            )
            senderApiProperty.set(
                retrofitProperty.map { retrofit ->
                    retrofit.build().create(TelegramWebhookSenderApi::class.java)
                },
            )
        }

        private val api: TelegramApi get() = apiProperty.get()
        private val senderApi: TelegramWebhookSenderApi get() = senderApiProperty.get()
        private val botId: String get() = parameters.botId.get()
        private val chatId: String get() = parameters.chatId.get()
        private val topicId: String? get() = parameters.topicId.orNull

        /**
         * Sends url formatted data to webhook at [webhookUrl]
         */
        fun send(message: String) {
            val topicId = this.topicId
            val webhookUrl =
                if (topicId.isNullOrEmpty()) {
                    SEND_MESSAGE_TO_CHAT_WEB_HOOK.format(
                        botId,
                        chatId,
                        URLEncoder.encode(message, "utf-8"),
                    )
                } else {
                    SEND_MESSAGE_TO_TOPIC_WEB_HOOK.format(
                        botId,
                        chatId,
                        topicId,
                        URLEncoder.encode(message, "utf-8"),
                    )
                }
            logger.info("sending changelog to $webhookUrl")
            senderApi.send(webhookUrl).executeOrThrow()
        }

        fun upload(file: File) {
            val webhookUrl = SEND_DOCUMENT_WEB_HOOK.format(botId)
            val filePart =
                MultipartBody.Part.createFormData(
                    "document",
                    file.name,
                    file.asRequestBody(),
                )
            val topicId = this.topicId
            val params =
                if (topicId != null) {
                    hashMapOf(
                        "message_thread_id" to createPartFromString(topicId),
                        "chat_id" to createPartFromString(chatId),
                    )
                } else {
                    hashMapOf(
                        "chat_id" to createPartFromString(chatId),
                    )
                }
            val response = api.upload(webhookUrl, params, filePart).executeOrThrow()
            if (!response.ok) {
                throw UploadException("Telegram uploading failed ${response.error_code}")
            }
        }

        companion object {
            private val logger: Logger = Logging.getLogger(TelegramNetworkService::class.java)
        }
    }
