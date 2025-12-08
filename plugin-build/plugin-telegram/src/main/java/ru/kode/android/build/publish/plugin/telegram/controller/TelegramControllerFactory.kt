package ru.kode.android.build.publish.plugin.telegram.controller

import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory

object TelegramControllerFactory {

    fun build(
        logger: Logger
    ): TelegramController {
        val json = Json { ignoreUnknownKeys = true }
        val client = TelegramClientFactory.build(logger, json)
        val retrofitBuilder = TelegramRetrofitBuilderFactory.build(client, json)
        return TelegramControllerImpl(
            webhookApi = TelegramWebhookApiFactory.build(retrofitBuilder),
            distributionApi = TelegramDistributionApiFactory.build(retrofitBuilder),
            logger = logger
        )
    }
}
