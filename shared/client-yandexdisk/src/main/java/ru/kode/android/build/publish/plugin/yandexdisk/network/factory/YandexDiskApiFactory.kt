package ru.kode.android.build.publish.plugin.yandexdisk.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.yandexdisk.network.api.YandexDiskApi

internal const val YANDEX_DISK_API_BASE_URL = "https://cloud-api.yandex.net/"

internal object YandexDiskApiFactory {
    fun build(
        client: OkHttpClient,
        baseUrl: String = YANDEX_DISK_API_BASE_URL,
    ): YandexDiskApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YandexDiskApi::class.java)
    }
}
