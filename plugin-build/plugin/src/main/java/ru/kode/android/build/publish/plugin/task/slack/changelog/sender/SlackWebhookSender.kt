package ru.kode.android.build.publish.plugin.task.slack.changelog.sender

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.slack.changelog.entity.SlackChangelogBody
import ru.kode.android.build.publish.plugin.task.slack.changelog.sender.api.SlackWebhookSenderApi
import ru.kode.android.build.publish.plugin.util.executeOrThrow
import java.util.concurrent.TimeUnit

internal class SlackWebhookSender(
    private val logger: Logger,
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
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
            .create(SlackWebhookSenderApi::class.java)

    /**
     * Sends [changelogBody] to a webhook at [webhookUrl]
     */
    fun send(
        webhookUrl: String,
        changelogBody: SlackChangelogBody,
    ) {
        logger.info("sending $changelogBody to $webhookUrl")
        api.send(webhookUrl, changelogBody).executeOrThrow()
    }
}

private const val HTTP_CONNECT_TIMEOUT_SEC = 60L

private const val STUB_BASE_URL = "http://localhost/"
