package ru.kode.android.build.publish.plugin.jira.network.factory

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi

internal object JiraApiFactory {

    fun build(client: OkHttpClient, baseUrl: String): JiraApi {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl("$baseUrl/rest/api/2/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(JiraApi::class.java)
    }
}