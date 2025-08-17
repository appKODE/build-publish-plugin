package ru.kode.android.build.publish.plugin.clickup.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService

/**
 * Extension that provides access to ClickUp network services for other plugins.
 *
 * This class is used internally by the ClickUp plugin to expose network services to other plugins.
 * It provides a way to access the configured [ClickUpNetworkService] instances by their names.
 */
abstract class ClickUpServiceExtension(
    /**
     * A provider of a map containing named [ClickUpNetworkService] providers.
     *
     * The keys in the map are the names of the configurations (e.g., "debug", "release"),
     * and the values are providers of the corresponding [ClickUpNetworkService] instances.
     *
     * This allows for lazy initialization of network services only when they are actually needed.
     */
    val networkServices: Provider<Map<String, Provider<ClickUpNetworkService>>>,
)
