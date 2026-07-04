package ru.kode.android.build.publish.plugin.telegram.controller

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramClientFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramDistributionApiFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.telegram.network.factory.TelegramWebhookApiFactory

object TelegramControllerFactory {
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

    fun build(log: (String) -> Unit = ::println): TelegramController = build(pluginLoggerFromLog(log))
}
