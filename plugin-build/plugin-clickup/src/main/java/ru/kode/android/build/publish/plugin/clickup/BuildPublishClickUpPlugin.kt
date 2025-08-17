@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.clickup

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.extension.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpServiceExtension
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService
import ru.kode.android.build.publish.plugin.core.util.serviceName

private const val EXTENSION_NAME = "buildPublishClickUp"
private const val NETWORK_SERVICE_NAME = "clickUpNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "clickUpNetworkServiceExtension"

/**
 * A Gradle plugin that integrates ClickUp task management with the Android build process.
 *
 * This plugin provides functionality to automate ClickUp tasks during the build process,
 * such as updating task statuses, adding tags, and setting custom fields.
 *
 * @see BuildPublishClickUpExtension For configuration options
 * @see ClickUpNetworkService For the underlying network operations
 */
abstract class BuildPublishClickUpPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishClickUpExtension::class.java)

        val androidExtension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<ClickUpNetworkService>>> =
                project.provider {
                    extension.auth.fold(mapOf()) { acc, authConfig ->
                        val service =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(NETWORK_SERVICE_NAME, authConfig.name),
                                ClickUpNetworkService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.token.set(authConfig.apiTokenFile)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, service)
                        }
                    }
                }

            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                ClickUpServiceExtension::class.java,
                services,
            )
        }
    }
}
