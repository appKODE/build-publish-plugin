package ru.kode.android.build.publish.plugin.slack.network.factory

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

/**
 * Factory for creating an [OkHttpClient] configured for Slack API requests.
 */
internal object SlackClientFactory {
    /**
     * Builds an [OkHttpClient] with logging and optional proxy support.
     *
     * @param logger the logger to use for logging HTTP requests and responses
     * @return an [OkHttpClient] instance configured for Slack API requests
     */
    fun build(logger: PluginLogger): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor { message ->
                if (!message.contains("Content-Disposition: form-data")) {
                    logger.info(message)
                }
            }.apply {
                level =
                    if (logger.bodyLogging) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.HEADERS
                    }
            }
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addProxyIfAvailable(logger)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}
