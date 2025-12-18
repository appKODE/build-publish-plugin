package ru.kode.android.build.publish.plugin.telegram.network.factory

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.telegram.messages.failedToParseRetryMessage
import ru.kode.android.build.publish.plugin.telegram.messages.reachedMaxTriesMessage
import ru.kode.android.build.publish.plugin.telegram.messages.tooManyRequestsMessage
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramErrorResponse
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val TOO_MANY_REQUESTS_ERROR_CODE = 429
private const val DEFAULT_RETRY_AFTER_SECONDS = 3L
private const val HALF_OF_SECOND_MS = 500L

/**
 * Factory for creating OkHttpClient instances with the necessary configuration for Telegram API communication.
 */
internal object TelegramClientFactory {

    /**
     * Builds an instance of OkHttpClient for Telegram API communication with the necessary
     * configuration.
     *
     * @param logger The logger used for logging HTTP requests and responses.
     * @param json The Json instance used for parsing error responses.
     * @return An instance of OkHttpClient.
     */
    fun build(logger: Logger, json: Json): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addProxyIfAvailable(logger)
            .addInterceptor(RetryAfterInterceptor(json, logger))
            .addInterceptor(DelayInterceptor(HALF_OF_SECOND_MS))
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}

private class DelayInterceptor(private val delayMs: Long) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        Thread.sleep(delayMs)
        return chain.proceed(chain.request())
    }
}

/**
 * Interceptor that retries requests that resulted in a 429 "Too Many Requests" error with a delay
 * specified in the "Retry-After" header.
 *
 * @param json The Json instance used for parsing error responses.
 * @param logger The logger used for logging HTTP requests and responses.
 * @param maxRetries The maximum number of retries. Default is 3.
 */
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
                logger.info(failedToParseRetryMessage(bodyString), e)
                DEFAULT_RETRY_AFTER_SECONDS
            }

            response.close()
            attempt++
            if (attempt <= maxRetries) {
                logger.info(tooManyRequestsMessage(retryAfterSeconds, attempt, maxRetries))
                Thread.sleep(TimeUnit.SECONDS.toMillis(retryAfterSeconds))
            } else {
                logger.info(reachedMaxTriesMessage(maxRetries))
            }

        } while (response.code == TOO_MANY_REQUESTS_ERROR_CODE && attempt <= maxRetries)

        return response
    }
}
