package ru.kode.android.build.publish.plugin.telegram.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

private const val STUB_BASE_URL = "http://localhost/"


/**
 * Factory for creating Retrofit.Builder instances with the necessary configuration for Telegram API communication.
 */
internal object TelegramRetrofitBuilderFactory {

    /**
     * Builds a [Retrofit.Builder] instance with the necessary configuration for Telegram API communication.
     *
     * @param client The OkHttpClient instance to use for network requests.
     * @param json The Json instance used for parsing error responses.
     * @return A new instance of [Retrofit.Builder].
     */
    fun build(client: OkHttpClient, json: Json): Retrofit.Builder {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(STUB_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
    }
}
