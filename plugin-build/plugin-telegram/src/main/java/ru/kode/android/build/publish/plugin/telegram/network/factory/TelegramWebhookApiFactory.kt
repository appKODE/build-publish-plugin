package ru.kode.android.build.publish.plugin.telegram.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi

/**
 * Factory for creating instances of [TelegramWebhookApi].
 */
internal object TelegramWebhookApiFactory {

    /**
     * Builds an instance of [TelegramWebhookApi] using the provided [Retrofit.Builder].
     *
     * @param retrofitBuilder The [Retrofit.Builder] to be used for building the [Retrofit] instance.
     * @return A new instance of [TelegramWebhookApi].
     */
    fun build(retrofitBuilder: Retrofit.Builder): TelegramWebhookApi {
        return retrofitBuilder.build().create(TelegramWebhookApi::class.java)
    }
}
