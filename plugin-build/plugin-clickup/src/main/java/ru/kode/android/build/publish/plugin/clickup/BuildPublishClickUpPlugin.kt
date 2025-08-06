@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.clickup

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.extensions.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpNetworkService
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpNetworkServiceExtension

private const val EXTENSION_NAME = "buildPublishClickUp"
private const val NETWORK_SERVICE_NAME = "clickUpNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "clickUpNetworkServiceExtension"

abstract class BuildPublishClickUpPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishClickUpExtension::class.java)

        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<ClickUpNetworkService>>> = project.provider {
                extension.auth.fold(mapOf()) { acc, authConfig ->
                    val service = project.gradle.sharedServices.registerIfAbsent(
                        networkServiceName(project, authConfig.name),
                        ClickUpNetworkService::class.java,
                        {
                            it.maxParallelUsages.set(1)
                            it.parameters.token.set(authConfig.apiTokenFile)
                        }
                    )
                    acc.toMutableMap().apply {
                        put(authConfig.name, service)
                    }
                }
            }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                ClickUpNetworkServiceExtension::class.java,
                services
            )
        }
    }
}

private fun networkServiceName(project: Project, buildName: String): String {
    return "${NETWORK_SERVICE_NAME}_${project.name}_${buildName}"
}
