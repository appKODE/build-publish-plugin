package ru.kode.android.build.publish.plugin.slack.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Factory for creating a preconfigured [Retrofit.Builder] for Slack API requests.
 */
internal object SlackRetrofitBuilderFactory {
    /**
     * Builds a [Retrofit.Builder] using the provided HTTP client and JSON serializer.
     */
    fun build(
        client: OkHttpClient,
        json: Json,
    ): Retrofit.Builder {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
    }
}
