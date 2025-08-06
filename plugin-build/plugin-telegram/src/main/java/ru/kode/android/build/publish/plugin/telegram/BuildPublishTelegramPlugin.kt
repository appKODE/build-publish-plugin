@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.telegram

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.telegram.extensions.BuildPublishTelegramExtension
import ru.kode.android.build.publish.plugin.telegram.service.TelegramNetworkService
import ru.kode.android.build.publish.plugin.telegram.service.TelegramNetworkServiceExtension

private const val EXTENSION_NAME = "buildPublishTelegram"
private const val NETWORK_SERVICE_NAME = "telegramNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "telegramNetworkServiceExtension"

abstract class BuildPublishTelegramPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishTelegramExtension::class.java)

        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<TelegramNetworkService>>> = project.provider {
                extension.bot.fold(mapOf()) { acc, authConfig ->
                    val service = project.gradle.sharedServices.registerIfAbsent(
                        networkServiceName(project, authConfig.name),
                        TelegramNetworkService::class.java
                    ) {
                        it.maxParallelUsages.set(1)
                        it.parameters.botId.set(authConfig.botId)
                        it.parameters.chatId.set(authConfig.chatId)
                        it.parameters.topicId.set(authConfig.topicId)
                    }
                    acc.toMutableMap().apply {
                        put(authConfig.name, service)
                    }
                }
            }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                TelegramNetworkServiceExtension::class.java,
                services
            )
        }
    }
}

private fun networkServiceName(project: Project, buildName: String): String {
    return "${NETWORK_SERVICE_NAME}_${project.name}_${buildName}"
}
