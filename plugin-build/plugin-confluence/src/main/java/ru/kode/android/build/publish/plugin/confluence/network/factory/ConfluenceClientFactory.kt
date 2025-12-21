package ru.kode.android.build.publish.plugin.confluence.network.factory

import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.EOFException
import okio.IOException
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.confluence.messages.eofDuringHandShakeMessage
import ru.kode.android.build.publish.plugin.confluence.messages.ioExceptionMessage
import ru.kode.android.build.publish.plugin.confluence.messages.sslHandShakeMessage
import ru.kode.android.build.publish.plugin.core.util.NetworkProxy
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

/**
 * Factory for creating OkHttpClient instances with the necessary configuration for Confluence API communication.
 */
internal object ConfluenceClientFactory {
    /**
     * Creates an OkHttpClient instance with the necessary configuration for Confluence API communication.
     *
     * @param username The username for basic authentication.
     * @param password The password for basic authentication.
     * @param logger The logger instance for logging.
     * @return The configured OkHttpClient instance.
     */
    fun build(
        username: String,
        password: String,
        logger: Logger,
    ): OkHttpClient {
        return buildClient(logger, username, password) {
            it.addProxyIfAvailable(logger)
        }
    }

    /**
     * Creates an OkHttpClient instance with the necessary configuration for Confluence API communication.
     *
     * @param username The username for basic authentication.
     * @param password The password for basic authentication.
     * @param logger The logger instance for logging.
     * @param proxy The proxy configuration for the OkHttpClient.
     * @return The configured OkHttpClient instance.
     */
    fun build(
        username: String,
        password: String,
        logger: Logger,
        proxy: () -> NetworkProxy?,
    ): OkHttpClient {
        return buildClient(logger, username, password) {
            it.addProxyIfAvailable(logger, proxy, proxy)
        }
    }
}

/**
 * Creates an OkHttpClient.Builder instance with the necessary configuration for Confluence API communication.
 *
 * @param logger The logger instance for logging.
 * @param username The username for basic authentication.
 * @param password The password for basic authentication.
 * @return The configured OkHttpClient.Builder instance.
 */
private fun buildClient(
    logger: Logger,
    username: String,
    password: String,
    apply: (OkHttpClient.Builder) -> OkHttpClient.Builder,
): OkHttpClient {
    val loggingInterceptor =
        HttpLoggingInterceptor { message -> logger.info(message) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    return OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
        .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .let(apply)
        .addInterceptor(AttachTokenInterceptor(username, password))
        .addInterceptor(RetryHandshakeInterceptor(logger))
        .addNetworkInterceptor(loggingInterceptor)
        .build()
}

/**
 * Interceptor that attaches the username and password as basic authentication
 * to each request.
 *
 * @property username The username used for authentication.
 * @property password The password used for authentication.
 */
private class AttachTokenInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Authorization", Credentials.basic(username, password))
                .build()
        return chain.proceed(newRequest)
    }
}

/**
 * Interceptor that retries handshake attempts with a delay specified between retries.
 *
 * @property maxRetries The maximum number of retries. Default is 3.
 * @property delayMillis The delay between retries in milliseconds. Default is 2000 milliseconds.
 * @property logger The logger instance used for logging.
 */
private class RetryHandshakeInterceptor(
    private val logger: Logger,
    private val maxRetries: Int = 3,
    private val delayMillis: Long = 2000,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                return chain.proceed(request)
            } catch (e: SSLHandshakeException) {
                lastException = e
                logger.info(sslHandShakeMessage(attempt, delayMillis, maxRetries), e)
            } catch (e: EOFException) {
                lastException = e
                logger.info(eofDuringHandShakeMessage(attempt, delayMillis, maxRetries), e)
            } catch (e: IOException) {
                lastException = e
                logger.info(ioExceptionMessage(attempt, delayMillis, maxRetries), e)
            }

            attempt++
            if (attempt <= maxRetries) {
                Thread.sleep(delayMillis)
            }
        }

        throw lastException ?: IOException("Unknown handshake failure after $maxRetries attempts")
    }
}
