package ru.kode.android.build.publish.plugin.telegram.controller

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory

/**
 * Factory for creating instances of [TelegramController].
 */
object TelegramControllerFactory {
    /**
     * Creates an instance of [TelegramController].
     *
     * @param logger The logger to be used by the controller.
     * @return A new instance of [TelegramController].
     */
    fun build(logger: PluginLogger): TelegramController {
        val json = Json { ignoreUnknownKeys = true }
        val client = TelegramClientFactory.build(logger, json)
        val retrofitBuilder = TelegramRetrofitBuilderFactory.build(client, json)
        return TelegramControllerImpl(
            webhookApi = TelegramWebhookApiFactory.build(retrofitBuilder),
            distributionApi = TelegramDistributionApiFactory.build(retrofitBuilder),
            logger = logger,
        )
    }
}
