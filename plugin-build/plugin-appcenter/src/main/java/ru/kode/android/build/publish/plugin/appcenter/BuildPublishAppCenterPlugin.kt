@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.appcenter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.extensions.BuildPublishAppCenterExtension
import ru.kode.android.build.publish.plugin.appcenter.service.AppCenterNetworkService
import ru.kode.android.build.publish.plugin.appcenter.service.AppCenterNetworkServiceExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

private const val EXTENSION_NAME = "buildPublishAppCenter"
private const val NETWORK_SERVICE_NAME = "appCenterNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "appCenterNetworkServiceExtension"

abstract class BuildPublishAppCenterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishAppCenterExtension::class.java)

        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<AppCenterNetworkService>>> = project.provider {
                extension.auth.fold(mapOf()) { acc, authConfig ->
                    val service = project.gradle.sharedServices.registerIfAbsent(
                        networkServiceName(project, authConfig.name),
                        AppCenterNetworkService::class.java,
                        {
                            it.maxParallelUsages.set(1)
                            it.parameters.token.set(authConfig.apiTokenFile)
                            it.parameters.ownerName.set(authConfig.ownerName)
                        }
                    )
                    acc.toMutableMap().apply {
                        put(authConfig.name, service)
                    }
                }
            }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                AppCenterNetworkServiceExtension::class.java,
                services
            )
        }
    }
}

private fun networkServiceName(project: Project, buildName: String): String {
    return "${NETWORK_SERVICE_NAME}_${project.name}_${buildName}"
}
