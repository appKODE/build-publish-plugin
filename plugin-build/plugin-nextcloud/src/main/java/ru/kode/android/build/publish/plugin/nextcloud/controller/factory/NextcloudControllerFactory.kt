package ru.kode.android.build.publish.plugin.nextcloud.controller.factory

import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.NetworkProxy
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudController
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudControllerImpl
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudApiFactory
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudClientFactory

object NextcloudControllerFactory {
    fun build(
        baseUrl: String,
        username: String,
        password: String,
        logger: PluginLogger,
        proxy: () -> NetworkProxy?,
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
}
