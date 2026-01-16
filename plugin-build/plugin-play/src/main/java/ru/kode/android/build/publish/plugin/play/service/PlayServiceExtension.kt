package ru.kode.android.build.publish.plugin.play.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.play.service.network.PlayNetworkService

/**
 * Extension class providing access to Google Play services for the build-publish plugin.
 *
 * This class serves as a container for Play Store related services and configurations.
 */
abstract class PlayServiceExtension(
    /**
     * Provider of a map containing named PlayNetworkService instances.
     *
     * The map keys represent service names, and the values are providers of PlayNetworkService instances.
     * This allows for lazy initialization and dependency injection of network services.
     */
    val networkServices: Provider<Map<String, Provider<PlayNetworkService>>>,
)
