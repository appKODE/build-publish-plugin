@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.play

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.play.extension.BuildPublishPlayExtension
import ru.kode.android.build.publish.plugin.play.service.PlayServiceExtension
import ru.kode.android.build.publish.plugin.play.service.network.PlayNetworkService

private const val EXTENSION_NAME = "buildPublishPlay"
private const val NETWORK_SERVICE_NAME = "playNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "playNetworkServiceExtension"

abstract class BuildPublishPlayPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishPlayExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<PlayNetworkService>>> =
                project.provider {
                    extension.auth.fold(mapOf()) { acc, authConfig ->
                        val service =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(NETWORK_SERVICE_NAME, authConfig.name),
                                PlayNetworkService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.appId.set(authConfig.appId)
                                    it.parameters.apiTokenFile.set(authConfig.apiTokenFile)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, service)
                        }
                    }
                }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                PlayServiceExtension::class.java,
                services,
            )
        }
    }
}
