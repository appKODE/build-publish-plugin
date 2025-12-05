package ru.kode.android.build.publish.plugin.telegram.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

private const val STUB_BASE_URL = "http://localhost/"

internal object TelegramRetrofitBuilderFactory {

    fun build(client: OkHttpClient): Retrofit.Builder {
       val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(STUB_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
    }
}
