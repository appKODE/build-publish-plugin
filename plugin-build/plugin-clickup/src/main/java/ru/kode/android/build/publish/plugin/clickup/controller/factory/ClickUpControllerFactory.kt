package ru.kode.android.build.publish.plugin.clickup.controller.factory

import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpControllerImpl
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpApiFactory
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpClientFactory
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger

object ClickUpControllerFactory {
    fun build(
        token: String,
        logger: PluginLogger,
    ): ClickUpController {
        return ClickUpControllerImpl(
            api =
                ClickUpApiFactory.build(
                    client = ClickUpClientFactory.build(token, logger),
                ),
            logger = logger,
        )
    }
}
