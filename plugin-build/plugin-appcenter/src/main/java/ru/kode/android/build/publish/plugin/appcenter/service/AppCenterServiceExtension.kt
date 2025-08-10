package ru.kode.android.build.publish.plugin.appcenter.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.service.network.AppCenterNetworkService

/**
 * Gradle extension that provides access to multiple AppCenter network services.
 *
 * @property networkServices A provider of a map where keys are service names
 *   and values are providers of [AppCenterNetworkService] instances.
 *   This allows accessing configured network services for different authentication contexts.
 */
abstract class AppCenterServiceExtension(
    val networkServices: Provider<Map<String, Provider<AppCenterNetworkService>>>,
)
