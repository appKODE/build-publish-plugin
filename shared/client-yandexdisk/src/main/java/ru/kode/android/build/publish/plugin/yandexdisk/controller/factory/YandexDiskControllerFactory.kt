package ru.kode.android.build.publish.plugin.yandexdisk.controller.factory

import ru.kode.android.build.publish.plugin.yandexdisk.controller.YandexDiskController
import ru.kode.android.build.publish.plugin.yandexdisk.controller.YandexDiskControllerImpl
import ru.kode.android.build.publish.plugin.yandexdisk.network.factory.YandexDiskApiFactory
import ru.kode.android.build.publish.plugin.yandexdisk.network.factory.YandexDiskClientFactory
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog

object YandexDiskControllerFactory {
    fun build(
        oauthToken: String,
        logger: PluginLogger,
    ): YandexDiskController =
        YandexDiskControllerImpl(
            api = YandexDiskApiFactory.build(client = YandexDiskClientFactory.build(oauthToken, logger)),
        )

    fun build(
        oauthToken: String,
        log: (String) -> Unit = ::println,
    ): YandexDiskController = build(oauthToken = oauthToken, logger = pluginLoggerFromLog(log))
}
