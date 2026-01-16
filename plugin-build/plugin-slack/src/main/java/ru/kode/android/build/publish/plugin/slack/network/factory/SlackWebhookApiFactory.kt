package ru.kode.android.build.publish.plugin.slack.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.slack.network.SlackApi

private const val STUB_BASE_URL = "http://localhost/"

/**
 * Factory for creating a [SlackApi] instance.
 *
 * Incoming webhooks are called via dynamic URLs, so a stub base URL is used to satisfy Retrofit.
 */
internal object SlackWebhookApiFactory {
    /**
     * Builds [SlackApi] using the provided [Retrofit.Builder].
     */
    fun build(retrofitBuilder: Retrofit.Builder): SlackApi {
        return retrofitBuilder
            .baseUrl(STUB_BASE_URL)
            .build()
            .create(SlackApi::class.java)
    }
}
