package ru.kode.android.build.publish.plugin.clickup.controller.factory

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpControllerImpl
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpApiFactory
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpClientFactory

object ClickUpControllerFactory {

    fun build(
        token: String,
        logger: Logger,
    ): ClickUpController {
        return ClickUpControllerImpl(
            api = ClickUpApiFactory.build(
                client = ClickUpClientFactory.build(token, logger),
            ),
            logger = logger
        )
    }
}
