package ru.kode.android.build.publish.plugin.vk.network.factory

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.util.addProxyIfAvailable
import ru.kode.android.gradle.commons.util.buildLoggingInterceptor
import java.util.concurrent.TimeUnit

internal const val VK_API_VERSION = "5.199"
private const val HTTP_TIMEOUT_MINUTES = 3L

internal object VkClientFactory {
    /**
     * Builds a client that adds the token (`Authorization: Bearer`) and the API version only to
     * requests for the API host ([apiBaseUrl]). The document upload URL is on another host and
     * carries its own signed parameters.
     */
    fun build(
        accessToken: String,
        logger: PluginLogger,
        apiBaseUrl: String = VK_API_BASE_URL,
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
                    chain.proceed(
                        request
                            .newBuilder()
                            .url(
                                request.url
                                    .newBuilder()
                                    .setQueryParameter("v", VK_API_VERSION)
                                    .build(),
                            ).header("Authorization", "Bearer $accessToken")
                            .build(),
                    )
                } else {
                    chain.proceed(request)
                }
            }.addNetworkInterceptor(buildLoggingInterceptor(logger))
            .build()
    }
}
