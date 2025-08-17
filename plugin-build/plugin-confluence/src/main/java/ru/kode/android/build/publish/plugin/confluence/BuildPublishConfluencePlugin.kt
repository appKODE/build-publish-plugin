@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.confluence

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.confluence.extension.BuildPublishConfluenceExtension
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceServiceExtension
import ru.kode.android.build.publish.plugin.confluence.service.network.ConfluenceNetworkService
import ru.kode.android.build.publish.plugin.core.util.serviceName

private const val EXTENSION_NAME = "buildPublishConfluence"
private const val NETWORK_SERVICE_NAME = "confluenceNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "confluenceNetworkServiceExtension"

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
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishConfluenceExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<ConfluenceNetworkService>>> =
                project.provider {
                    extension.auth.fold(mapOf()) { acc, authConfig ->
                        val service =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(NETWORK_SERVICE_NAME, authConfig.name),
                                ConfluenceNetworkService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.credentials.set(authConfig.credentials)
                                    it.parameters.baseUrl.set(authConfig.baseUrl)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, service)
                        }
                    }
                }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                ConfluenceServiceExtension::class.java,
                services,
            )
        }
    }
}
