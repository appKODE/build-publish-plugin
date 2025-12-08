package ru.kode.android.build.publish.plugin.telegram.network.factory

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramErrorResponse
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val TOO_MANY_REQUESTS_ERROR_CODE = 429
private const val DEFAULT_RETRY_AFTER_SECONDS = 3L

internal object TelegramClientFactory {

    fun build(logger: Logger, json: Json): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addProxyIfAvailable()
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(loggingInterceptor)
                addInterceptor(RetryAfterInterceptor(json, logger))
            }
            .build()
    }
}

private class RetryAfterInterceptor(
    private val json: Json,
    private val logger: Logger,
    private val maxRetries: Int = 3,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var response: Response

        do {
            response = chain.proceed(request)

            if (response.code != TOO_MANY_REQUESTS_ERROR_CODE) {
                return response
            }

            val bodyString = response.body.string()
            val retryAfterSeconds = try {
                val errorResponse = json.decodeFromString<TelegramErrorResponse>(bodyString)
                errorResponse.parameters?.retry_after ?: DEFAULT_RETRY_AFTER_SECONDS
            } catch (e: Exception) {
                logger.info("Failed to parse retry_after from response: $bodyString", e)
                DEFAULT_RETRY_AFTER_SECONDS
            }

            response.close()
            attempt++
            if (attempt <= maxRetries) {
                logger.info("429 Too Many Requests, retrying after $retryAfterSeconds seconds (attempt $attempt/$maxRetries)...")
                Thread.sleep(TimeUnit.SECONDS.toMillis(retryAfterSeconds))
            } else {
                logger.info("Reached max retries ($maxRetries). Returning 429 response.")
            }

        } while (response.code == TOO_MANY_REQUESTS_ERROR_CODE && attempt <= maxRetries)

        return response
    }
}
