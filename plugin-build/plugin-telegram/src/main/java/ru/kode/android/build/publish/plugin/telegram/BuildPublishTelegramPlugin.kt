@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.telegram

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.mapToEntity
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.toJson
import ru.kode.android.build.publish.plugin.telegram.extension.BuildPublishTelegramExtension
import ru.kode.android.build.publish.plugin.telegram.messages.extensionCreatedMessage
import ru.kode.android.build.publish.plugin.telegram.messages.mustApplyFoundationPluginMessage
import ru.kode.android.build.publish.plugin.telegram.messages.noBotsConfiguredMessage
import ru.kode.android.build.publish.plugin.telegram.messages.registeringServiceMessage
import ru.kode.android.build.publish.plugin.telegram.messages.telegramServicesCreated
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import ru.kode.android.build.publish.plugin.telegram.service.TelegramServiceExtension

internal const val EXTENSION_NAME = "buildPublishTelegram"
internal const val SERVICE_EXTENSION_NAME = "telegramServiceExtension"

private const val SERVICE_NAME = "telegramService"

/**
 * A Gradle plugin that integrates with Telegram to send build notifications and deployment updates.
 *
 * This plugin provides seamless integration with Telegram's Bot API to send notifications about
 * build status, deployment progress, and changelogs. It's designed to work with the Android
 * build system and can be configured to send messages at different stages of the build process.
 *
 * @see BuildPublishTelegramExtension For available configuration options
 * @see TelegramService For the underlying network service implementation
 */
abstract class BuildPublishTelegramPlugin : Plugin<Project> {
    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishTelegramExtension::class.java,
            )

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java,
            )
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            TelegramServiceExtension::class.java,
            servicesProperty,
        )

        logger.info(extensionCreatedMessage())

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(mustApplyFoundationPluginMessage())
        }

        val androidExtension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            if (extension.bots.isEmpty()) {
                logger.info(noBotsConfiguredMessage())
                return@finalizeDsl
            }

            logger.info(registeringServiceMessage())

            val serviceMap =
                extension.bots.associate { botConfig ->
                    val name = botConfig.name
                    val service =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            TelegramService::class.java,
                        ) { spec ->
                            spec.maxParallelUsages.set(1)
                            spec.parameters.bots.set(botConfig.bots.map { it.mapToEntity().toJson() })
                        }
                    name to service
                }

            logger.info(telegramServicesCreated(serviceMap.keys))

            servicesProperty.set(serviceMap)
        }
    }
}
