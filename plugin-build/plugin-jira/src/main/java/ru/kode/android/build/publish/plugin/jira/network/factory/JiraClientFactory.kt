package ru.kode.android.build.publish.plugin.jira.network.factory

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 30L

/**
 * Factory for creating OkHttpClient instances with the necessary configuration for Jira API communication.
 */
internal object JiraClientFactory {
    /**
     * Builds an instance of OkHttpClient for Jira API communication with the necessary configuration.
     *
     * @param username The username used for authentication with the Jira API.
     * @param password The password used for authentication with the Jira API.
     * @param logger The logger used for logging HTTP requests and responses.
     * @return An instance of OkHttpClient.
     */
    fun build(
        username: String,
        password: String,
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
            .addInterceptor(AttachTokenInterceptor(username, password))
            .addProxyIfAvailable(logger)
            .addNetworkInterceptor(loggingInterceptor)
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
    override fun intercept(chain: Interceptor.Chain) =
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader("Authorization", Credentials.basic(username, password))
                .build(),
        )
}
