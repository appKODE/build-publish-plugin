package ru.kode.android.build.publish.plugin.telegram.task.changelog.sender

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.telegram.task.changelog.sender.api.TelegramWebhookSenderApi
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import java.util.concurrent.TimeUnit

internal class TelegramWebhookSender(
    private val logger: Logger,
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addProxyIfAvailable()
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(loggingInterceptor)
            }
            .build()

    private val moshi = Moshi.Builder().build()

    private val api =
        Retrofit.Builder()
            .baseUrl(STUB_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramWebhookSenderApi::class.java)

    /**
     * Sends url formatted data to webhook at [webhookUrl]
     */
    fun send(
        webhookUrl: String,
        authorization: String?,
    ) {
        logger.info("sending changelog to $webhookUrl")
        api.send(authorization, webhookUrl).executeOrThrow()
    }
}

private const val HTTP_CONNECT_TIMEOUT_SEC = 60L

private const val STUB_BASE_URL = "http://localhost/"
