package ru.kode.android.build.publish.plugin.clickup.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.clickup.network.api.ClickUpApi

const val CLICK_UP_API_BASE_URL = "https://api.clickup.com/api/"
private const val MEDIA_TYPE_JSON = "application/json"

/**
 * Factory for creating a [ClickUpApi] Retrofit client.
 */
internal object ClickUpApiFactory {
    fun build(
        client: OkHttpClient,
        baseUrl: String = CLICK_UP_API_BASE_URL,
    ): ClickUpApi {
        val contentType = MEDIA_TYPE_JSON.toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ClickUpApi::class.java)
    }
}
