package ru.kode.android.build.publish.plugin.jira.network.factory

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi

/**
 * Factory for creating instances of [JiraApi].
 */
internal object JiraApiFactory {

    /**
     * Builds an instance of [JiraApi] using the provided [OkHttpClient] and [baseUrl].
     *
     * @param client The [OkHttpClient] to be used for network requests.
     * @param baseUrl The base URL of the Jira instance.
     * @return A new instance of [JiraApi].
     */
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