package ru.kode.android.build.publish.plugin.confluence.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.confluence.network.api.ConfluenceApi

/**
 * Factory for creating instances of [ConfluenceApi].
 */
internal object ConfluenceApiFactory {
    /**
     * Builds an instance of [ConfluenceApi] using the provided [OkHttpClient] and [baseUrl].
     *
     * @param client The [OkHttpClient] to be used for network requests.
     * @param baseUrl The base URL of the Confluence instance.
     * @return A new instance of [ConfluenceApi].
     */
    fun build(
        client: OkHttpClient,
        baseUrl: String,
    ): ConfluenceApi {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ConfluenceApi::class.java)
    }
}
