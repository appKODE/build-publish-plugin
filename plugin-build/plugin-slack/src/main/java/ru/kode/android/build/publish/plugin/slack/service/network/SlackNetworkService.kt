package ru.kode.android.build.publish.plugin.slack.service.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val logger: Logger = Logging.getLogger(SlackUploadService::class.java)

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

/**
 * A network service for interacting with the Slack API.
 *
 * This service provides the underlying HTTP client and Retrofit configuration
 * for Slack API communication. It's designed to be used as a shared service
 * across different Slack-related tasks to manage resources efficiently.
 */
abstract class SlackNetworkService
    @Inject
    constructor(
        providerFactory: ProviderFactory,
    ) : BuildService<BuildServiceParameters.None> {
        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val retrofitProperty: Property<Retrofit.Builder>

        internal abstract val moshiProperty: Property<Moshi>

        init {
            okHttpClientProperty.set(
                OkHttpClient.Builder()
                    .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .addProxyIfAvailable()
                    .apply {
                        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                        addNetworkInterceptor(loggingInterceptor)
                    }
                    .build(),
            )
            moshiProperty.set(providerFactory.provider { Moshi.Builder().build() })
            retrofitProperty.set(
                okHttpClientProperty.zip(moshiProperty) { client, moshi ->
                    Retrofit.Builder()
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                },
            )
        }
    }
