package ru.kode.android.build.publish.plugin.slack.network.factory

import okhttp3.OkHttpClient
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.buildLoggingInterceptor
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

internal object SlackClientFactory {
    fun build(logger: PluginLogger): OkHttpClient {
        val loggingInterceptor = buildLoggingInterceptor(logger)
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addProxyIfAvailable(logger)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}
