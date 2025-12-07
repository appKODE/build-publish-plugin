package ru.kode.android.build.publish.plugin.telegram.controller

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory

object TelegramControllerFactory {

    fun build(
        logger: Logger
    ): TelegramController {
        val client = TelegramClientFactory.build(logger)
        val retrofitBuilder = TelegramRetrofitBuilderFactory.build(client)
        return TelegramControllerImpl(
            webhookApi = TelegramWebhookApiFactory.build(retrofitBuilder),
            distributionApi = TelegramDistributionApiFactory.build(retrofitBuilder),
            logger = logger
        )
    }
}
