@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.confluence

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.confluence.extension.BuildPublishConfluenceExtension
import ru.kode.android.build.publish.plugin.confluence.messages.extensionCreatedMessage
import ru.kode.android.build.publish.plugin.confluence.messages.mustBeUsedWithFoundationPluginException
import ru.kode.android.build.publish.plugin.confluence.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.confluence.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.confluence.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceServiceExtension
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.core.util.serviceName

private const val EXTENSION_NAME = "buildPublishConfluence"
private const val SERVICE_NAME = "confluenceService"
private const val SERVICE_EXTENSION_NAME = "confluenceServiceExtension"

/**
 * A Gradle plugin that provides Confluence integration for build publishing.
 *
 * This plugin enables:
 * - Uploading build artifacts to Confluence pages
 * - Managing Confluence content as part of the build process
 * - Authentication with multiple Confluence instances
 *
 * It sets up the necessary services and extensions for Confluence integration,
 * including network services for API communication.
 */
abstract class BuildPublishConfluencePlugin : Plugin<Project> {

    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishConfluenceExtension::class.java
            )

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java
            )
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            ConfluenceServiceExtension::class.java,
            servicesProperty
        )

        logger.info(extensionCreatedMessage())

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(mustBeUsedWithFoundationPluginException())
        }

        val androidExtension =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            if (extension.auth.isEmpty()) {
                logger.info(noAuthConfigsMessage())
                return@finalizeDsl
            }

            logger.info(registeringServicesMessage())

            val serviceMap =
                extension.auth.associate { authConfig ->
                    val name = authConfig.name

                    val service =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            ConfluenceService::class.java
                        ) {
                            it.maxParallelUsages.set(1)
                            it.parameters.credentials.set(authConfig.credentials)
                            it.parameters.baseUrl.set(authConfig.baseUrl)
                        }

                    name to service
                }

            logger.info(servicesCreatedMessage(serviceMap.keys))
            servicesProperty.set(serviceMap)
        }
    }
}
