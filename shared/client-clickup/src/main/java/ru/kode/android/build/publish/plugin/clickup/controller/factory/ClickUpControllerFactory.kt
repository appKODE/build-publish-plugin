package ru.kode.android.build.publish.plugin.clickup.controller.factory

import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpControllerImpl
import ru.kode.android.build.publish.plugin.clickup.network.factory.CLICK_UP_API_BASE_URL
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpApiFactory
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpClientFactory
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog

/**
 * Factory for constructing a fully configured [ClickUpController] instance.
 *
 * [baseUrl] defaults to the public ClickUp API; it is a parameter so tests can point the controller at a
 * local mock server (ClickUp has a single, non-self-hosted host, so production always uses the default).
 */
object ClickUpControllerFactory {
    fun build(
        token: String,
        logger: PluginLogger,
        baseUrl: String = CLICK_UP_API_BASE_URL,
    ): ClickUpController {
        return ClickUpControllerImpl(
            api =
                ClickUpApiFactory.build(
                    client = ClickUpClientFactory.build(token, logger),
                    baseUrl = baseUrl,
                ),
            logger = logger,
        )
    }

    fun build(
        token: String,
        log: (String) -> Unit = ::println,
        baseUrl: String = CLICK_UP_API_BASE_URL,
    ): ClickUpController =
        build(
            token = token,
            logger = pluginLoggerFromLog(log),
            baseUrl = baseUrl,
        )
}
