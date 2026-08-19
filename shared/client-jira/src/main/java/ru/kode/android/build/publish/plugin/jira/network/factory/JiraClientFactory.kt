package ru.kode.android.build.publish.plugin.jira.network.factory

import okhttp3.OkHttpClient
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.util.addProxyIfAvailable
import ru.kode.android.gradle.commons.util.buildBasicAuthInterceptor
import ru.kode.android.gradle.commons.util.buildLoggingInterceptor
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
        val loggingInterceptor = buildLoggingInterceptor(logger)
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(
                buildBasicAuthInterceptor(
                    username = username,
                    password = password,
                    extraHeaders = mapOf("Content-Type" to "application/json"),
                ),
            )
            .addProxyIfAvailable(logger)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}
