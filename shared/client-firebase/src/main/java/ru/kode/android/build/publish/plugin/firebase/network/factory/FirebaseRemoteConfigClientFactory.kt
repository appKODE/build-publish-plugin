package ru.kode.android.build.publish.plugin.firebase.network.factory

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import ru.kode.android.build.publish.plugin.firebase.network.entity.ServiceAccountKey
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.util.addProxyIfAvailable
import ru.kode.android.gradle.commons.util.buildLoggingInterceptor
import java.util.concurrent.TimeUnit

private const val HTTP_TIMEOUT_MINUTES = 2L

internal object FirebaseRemoteConfigClientFactory {
    fun build(
        key: ServiceAccountKey,
        logger: PluginLogger,
        json: Json,
    ): OkHttpClient {
        val tokenClient =
            OkHttpClient.Builder()
                .connectTimeout(HTTP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .readTimeout(HTTP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .addProxyIfAvailable(logger)
                .build()
        return tokenClient.newBuilder()
            .addInterceptor(ServiceAccountTokenInterceptor(key, tokenClient, json))
            .addNetworkInterceptor(buildLoggingInterceptor(logger))
            .build()
    }
}
