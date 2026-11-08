package ru.kode.android.build.publish.plugin.yandexdisk.network.factory

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.util.addProxyIfAvailable
import ru.kode.android.gradle.commons.util.buildLoggingInterceptor
import java.util.concurrent.TimeUnit

private const val HTTP_TIMEOUT_MINUTES = 3L

internal object YandexDiskClientFactory {
    /**
     * Builds a client that sends `Authorization: OAuth <token>` only to the REST API host
     * ([apiBaseUrl]). The upload URL returned by the API lives on another host and must not
     * receive the token.
     */
    fun build(
        oauthToken: String,
        logger: PluginLogger,
        apiBaseUrl: String = YANDEX_DISK_API_BASE_URL,
    ): OkHttpClient {
        val apiHost = apiBaseUrl.toHttpUrl().host
        return OkHttpClient
            .Builder()
            .connectTimeout(HTTP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addProxyIfAvailable(logger)
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == apiHost) {
                    chain.proceed(request.newBuilder().header("Authorization", "OAuth $oauthToken").build())
                } else {
                    chain.proceed(request)
                }
            }.addNetworkInterceptor(buildLoggingInterceptor(logger))
            .build()
    }
}
