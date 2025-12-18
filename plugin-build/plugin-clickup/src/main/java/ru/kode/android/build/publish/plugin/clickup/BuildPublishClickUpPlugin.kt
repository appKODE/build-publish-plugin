@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.clickup

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.clickup.extension.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.messages.extensionCreatedMessage
import ru.kode.android.build.publish.plugin.clickup.messages.mustApplyFoundationPluginMessage
import ru.kode.android.build.publish.plugin.clickup.messages.noAuthConfigMessage
import ru.kode.android.build.publish.plugin.clickup.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.clickup.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpServiceExtension
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin

private const val EXTENSION_NAME = "buildPublishClickUp"
private const val SERVICE_NAME = "clickUpService"
private const val SERVICE_EXTENSION_NAME = "clickUpServiceExtension"

/**
 * A Gradle plugin that integrates ClickUp task management with the Android build process.
 *
 * This plugin provides functionality to automate ClickUp tasks during the build process,
 * such as updating task statuses, adding tags, and setting custom fields.
 *
 * @see BuildPublishClickUpExtension For configuration options
 * @see ClickUpService For the underlying network operations
 */
abstract class BuildPublishClickUpPlugin : Plugin<Project> {

    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishClickUpExtension::class.java
            )

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java
            )
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            ClickUpServiceExtension::class.java,
            servicesProperty
        )

        logger.info(extensionCreatedMessage())

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(mustApplyFoundationPluginMessage())
        }

        val androidExtension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            if (extension.auth.isEmpty()) {
                logger.info(noAuthConfigMessage())
                return@finalizeDsl
            }

            logger.info(registeringServicesMessage())

            val serviceMap =
                extension.auth.associate { authConfig ->
                    val name = authConfig.name

                    val service =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            ClickUpService::class.java
                        ) {
                            it.maxParallelUsages.set(1)
                            it.parameters.token.set(authConfig.apiTokenFile)
                        }

                    name to service
                }

            logger.info(servicesCreatedMessage(serviceMap.keys))

            servicesProperty.set(serviceMap)
        }
    }
}
