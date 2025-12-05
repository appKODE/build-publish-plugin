package ru.kode.android.build.publish.plugin.telegram.network.factory

import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi

internal object TelegramDistributionApiFactory {

    fun build(retrofitBuilder: Retrofit.Builder): TelegramDistributionApi {
        return retrofitBuilder.build().create(TelegramDistributionApi::class.java)
    }
}
