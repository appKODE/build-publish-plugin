package ru.kode.android.build.publish.plugin.jira.network.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi

private const val MEDIA_TYPE_JSON = "application/json"
private const val JIRA_API_PATH = "/rest/api/2/"

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
    fun build(
        client: OkHttpClient,
        baseUrl: String,
    ): JiraApi {
        val contentType = MEDIA_TYPE_JSON.toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl("$baseUrl$JIRA_API_PATH")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(JiraApi::class.java)
    }
}
