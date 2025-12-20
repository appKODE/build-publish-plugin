package ru.kode.android.build.publish.plugin.slack.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.slack.network.SlackApi

private const val STUB_BASE_URL = "http://localhost/"

internal object SlackWebhookApiFactory {
    fun build(retrofitBuilder: Retrofit.Builder): SlackApi {
        return retrofitBuilder
            .baseUrl(STUB_BASE_URL)
            .build()
            .create(SlackApi::class.java)
    }
}
