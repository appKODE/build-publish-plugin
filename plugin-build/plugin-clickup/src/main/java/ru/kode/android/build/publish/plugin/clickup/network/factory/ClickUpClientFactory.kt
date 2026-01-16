package ru.kode.android.build.publish.plugin.clickup.network.factory

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L

/**
 * Factory for creating an [OkHttpClient] configured for ClickUp API calls.
 */
internal object ClickUpClientFactory {
    fun build(
        token: String,
        logger: PluginLogger,
    ): OkHttpClient {
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
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AttachTokenInterceptor(token))
            .addProxyIfAvailable(logger)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}

private class AttachTokenInterceptor(
    private val apiToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader(name = "Authorization", apiToken)
                .build()
        return chain.proceed(newRequest)
    }
}
