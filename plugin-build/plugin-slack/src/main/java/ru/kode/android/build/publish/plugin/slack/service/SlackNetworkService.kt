package ru.kode.android.build.publish.plugin.slack.service

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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

abstract class SlackNetworkService @Inject constructor(
    providerFactory: ProviderFactory
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
                .build()
        )
        moshiProperty.set(providerFactory.provider { Moshi.Builder().build() })
        retrofitProperty.set(
            okHttpClientProperty.zip(moshiProperty) { client, moshi ->
                Retrofit.Builder()
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
            }
        )
    }

    companion object {
        private val logger: Logger = Logging.getLogger(SlackUploadService::class.java)
    }
}
