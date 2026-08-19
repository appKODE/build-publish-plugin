package ru.kode.android.build.publish.plugin.nextcloud.controller.factory

import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudController
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudControllerImpl
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudApiFactory
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudClientFactory
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog
import ru.kode.android.gradle.commons.util.NetworkProxy

object NextcloudControllerFactory {
    fun build(
        baseUrl: String,
        username: String,
        password: String,
        logger: PluginLogger,
        proxy: () -> NetworkProxy? = { null },
    ): NextcloudController {
        return NextcloudControllerImpl(
            baseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/",
            username = username,
            api =
                NextcloudApiFactory.build(
                    client = NextcloudClientFactory.build(username, password, logger, proxy),
                    baseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/",
                ),
        )
    }

    fun build(
        baseUrl: String,
        username: String,
        password: String,
        log: (String) -> Unit = ::println,
    ): NextcloudController =
        build(
            baseUrl = baseUrl,
            username = username,
            password = password,
            logger = pluginLoggerFromLog(log),
            proxy = { null },
        )
}
