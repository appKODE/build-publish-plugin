package ru.kode.android.build.publish.plugin.slack.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.slack.network.SlackUploadApi

private const val SLACK_BASE_URL = "https://slack.com/api/"

/**
 * Factory for creating a [SlackUploadApi] instance.
 */
internal object SlackUploadApiFactory {
    /**
     * Builds [SlackUploadApi] using the provided [Retrofit.Builder].
     */
    fun build(retrofitBuilder: Retrofit.Builder): SlackUploadApi {
        return retrofitBuilder
            .baseUrl(SLACK_BASE_URL)
            .build()
            .create(SlackUploadApi::class.java)
    }
}
