package ru.kode.android.build.publish.plugin.nextcloud.service

import org.gradle.api.provider.Provider

/**
 * Extension for configuring and accessing Nextcloud services within the build.
 *
 * This class provides access to configured Nextcloud network services that can be used
 * to interact with Nextcloud's API. The services are provided as a map where the key is
 * the service name and the value is a provider for the network service instance.
 */
abstract class NextcloudServiceExtension(
    /**
     * A provider of a map containing Nextcloud network service providers,
     * keyed by service name.
     */
    val services: Provider<Map<String, Provider<NextcloudService>>>,
)
