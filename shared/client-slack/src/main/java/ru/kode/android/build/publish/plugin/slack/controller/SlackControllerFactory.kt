package ru.kode.android.build.publish.plugin.slack.controller

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackClientFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackUploadApiFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackWebhookApiFactory
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog

object SlackControllerFactory {
    fun build(logger: PluginLogger): SlackController {
        val json = Json { ignoreUnknownKeys = true }
        val client = SlackClientFactory.build(logger)
        val retrofitBuilder = SlackRetrofitBuilderFactory.build(client, json)
        return SlackControllerImpl(
            json = json,
            slackApi = SlackWebhookApiFactory.build(retrofitBuilder),
            uploadApi = SlackUploadApiFactory.build(retrofitBuilder),
            logger = logger,
        )
    }

    fun build(log: (String) -> Unit = ::println): SlackController = build(pluginLoggerFromLog(log))
}
