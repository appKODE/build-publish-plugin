package ru.kode.android.build.publish.plugin.telegram.controller

import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory

object TelegramControllerFactory {

    fun build(): TelegramController {
        val client = TelegramClientFactory.build()
        val retrofitBuilder = TelegramRetrofitBuilderFactory.build(client)
        return TelegramControllerImpl(
            webhookApi = TelegramWebhookApiFactory.build(
                retrofitBuilder
            ),
            distributionApi = TelegramDistributionApiFactory.build(retrofitBuilder)
        )
    }
}
