@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.appcenter

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.extension.BuildPublishAppCenterExtension
import ru.kode.android.build.publish.plugin.appcenter.service.AppCenterServiceExtension
import ru.kode.android.build.publish.plugin.appcenter.service.network.AppCenterNetworkService
import ru.kode.android.build.publish.plugin.core.util.serviceName

private const val EXTENSION_NAME = "buildPublishAppCenter"
private const val NETWORK_SERVICE_NAME = "appCenterNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "appCenterNetworkServiceExtension"

/**
 * Gradle plugin that wires App Center configuration and network services into the Android build.
 *
 * How it works:
 *
 * 1. **Registers the main extension**:
 *    - Creates [BuildPublishAppCenterExtension] under the name `buildPublishAppCenter`.
 *    - This is where users define `auth` and `distribution` configs in `build.gradle`.
 *
 * 2. **Hooks into Android build lifecycle**:
 *    - Obtains [ApplicationAndroidComponentsExtension] to run logic after the Android DSL is finalized (`finalizeDsl`).
 *    - At this point, all build types and flavors are already known.
 *
 * 3. **Creates App Center network services**:
 *    - Iterates over all `auth` configurations from the extension.
 *    - For each `authConfig`, registers a [AppCenterNetworkService] as a **Gradle shared service**.
 *      - Shared services are reused across tasks instead of being recreated for each execution.
 *      - Each service is configured with:
 *        - `apiTokenFile` — authentication token file for App Center.
 *        - `ownerName` — owner of the App Center project.
 *      - `maxParallelUsages` is set to 1 to prevent concurrent API calls with the same credentials.
 *
 * 4. **Exposes network services to the build**:
 *    - Creates [AppCenterServiceExtension] (`appCenterNetworkServiceExtension`) containing a map
 *          of service providers, keyed by `authConfig.name`.
 *    - This lets other tasks or plugins retrieve the correct network service for a given build type.
 *
 * In short:
 * - Users configure `auth` and `distribution` in the Gradle script.
 * - The plugin automatically registers one App Center network service per `auth` configuration.
 * - These services can then be injected into tasks for publishing builds to App Center.
 */
abstract class BuildPublishAppCenterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishAppCenterExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<AppCenterNetworkService>>> =
                project.provider {
                    extension.auth.fold(mapOf()) { acc, authConfig ->
                        val service =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(NETWORK_SERVICE_NAME, authConfig.name),
                                AppCenterNetworkService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.token.set(authConfig.apiTokenFile)
                                    it.parameters.ownerName.set(authConfig.ownerName)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, service)
                        }
                    }
                }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                AppCenterServiceExtension::class.java,
                services,
            )
        }
    }
}
