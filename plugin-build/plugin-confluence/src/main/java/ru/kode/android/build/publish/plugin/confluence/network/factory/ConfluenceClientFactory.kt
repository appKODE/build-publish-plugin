package ru.kode.android.build.publish.plugin.confluence.network.factory

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.NetworkProxy
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit

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
       return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addInterceptor(AttachTokenInterceptor(username, password))
            .addProxyIfAvailable(logger)
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(loggingInterceptor)
            }
            .build()
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
        proxy: () -> NetworkProxy?
    ): OkHttpClient {
       return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addInterceptor(AttachTokenInterceptor(username, password))
            .addProxyIfAvailable(logger, proxy, proxy)
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(loggingInterceptor)
            }
            .build()
    }
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
