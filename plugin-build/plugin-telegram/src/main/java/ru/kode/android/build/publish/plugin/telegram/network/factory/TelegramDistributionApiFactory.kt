package ru.kode.android.build.publish.plugin.telegram.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi

/**
 * Factory for creating instances of [TelegramDistributionApi].
 */
internal object TelegramDistributionApiFactory {

    /**
     * Builds an instance of [TelegramDistributionApi] using the provided [Retrofit.Builder].
     *
     * @param retrofitBuilder The [Retrofit.Builder] to be used for creating the instance.
     * @return A new instance of [TelegramDistributionApi].
     */
    fun build(retrofitBuilder: Retrofit.Builder): TelegramDistributionApi {
        return retrofitBuilder.build().create(TelegramDistributionApi::class.java)
    }
}
