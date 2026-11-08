package ru.kode.android.build.publish.plugin.vk.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.vk.network.api.VkApi

internal const val VK_API_BASE_URL = "https://api.vk.com/method/"

internal object VkApiFactory {
    fun build(
        client: OkHttpClient,
        baseUrl: String = VK_API_BASE_URL,
    ): VkApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(VkApi::class.java)
    }
}
