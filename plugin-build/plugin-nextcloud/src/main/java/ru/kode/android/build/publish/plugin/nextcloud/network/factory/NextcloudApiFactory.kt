package ru.kode.android.build.publish.plugin.nextcloud.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.nextcloud.network.api.NextcloudApi

/**
 * Factory for creating instances of [NextcloudApi].
 */
internal object NextcloudApiFactory {
    /**
     * Builds an instance of [NextcloudApi] using the provided [OkHttpClient] and [baseUrl].
     *
     * @param client The [OkHttpClient] to be used for network requests.
     * @param baseUrl The base URL of the Nextcloud instance.
     * @return A new instance of [NextcloudApi].
     */
    fun build(
        client: OkHttpClient,
        baseUrl: String,
    ): NextcloudApi {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NextcloudApi::class.java)
    }
}
