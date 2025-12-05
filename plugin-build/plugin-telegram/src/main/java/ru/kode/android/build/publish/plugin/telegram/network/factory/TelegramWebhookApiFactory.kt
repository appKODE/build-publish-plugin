package ru.kode.android.build.publish.plugin.telegram.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi

internal object TelegramWebhookApiFactory {

    fun build(retrofitBuilder: Retrofit.Builder): TelegramWebhookApi {
        return retrofitBuilder.build().create(TelegramWebhookApi::class.java)
    }
}
