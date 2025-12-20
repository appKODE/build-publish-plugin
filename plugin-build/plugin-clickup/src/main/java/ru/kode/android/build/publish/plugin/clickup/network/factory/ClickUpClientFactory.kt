package ru.kode.android.build.publish.plugin.clickup.network.factory

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L

internal object ClickUpClientFactory {
    fun build(
        token: String,
        logger: Logger,
    ): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor { message -> logger.info(message) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        return OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AttachTokenInterceptor(token))
            .addProxyIfAvailable()
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}

private class AttachTokenInterceptor(
    private val apiToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader(name = "Authorization", apiToken)
                .build()
        return chain.proceed(newRequest)
    }
}
