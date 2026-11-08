package ru.kode.android.build.publish.plugin.vk.controller.factory

import ru.kode.android.build.publish.plugin.vk.controller.VkController
import ru.kode.android.build.publish.plugin.vk.controller.VkControllerImpl
import ru.kode.android.build.publish.plugin.vk.network.factory.VkApiFactory
import ru.kode.android.build.publish.plugin.vk.network.factory.VkClientFactory
import ru.kode.android.gradle.commons.logger.PluginLogger
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog

object VkControllerFactory {
    fun build(
        accessToken: String,
        logger: PluginLogger,
    ): VkController = VkControllerImpl(api = VkApiFactory.build(client = VkClientFactory.build(accessToken, logger)))

    fun build(
        accessToken: String,
        log: (String) -> Unit = ::println,
    ): VkController = build(accessToken = accessToken, logger = pluginLoggerFromLog(log))
}
