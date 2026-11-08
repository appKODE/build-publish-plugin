package ru.kode.android.build.publish.plugin.vk.service

import org.gradle.api.provider.Provider

/**
 * Extension for configuring and accessing VK services within the build.
 *
 * This class provides access to configured VK network services that can be used
 * to interact with VK's API. The services are provided as a map where the key is
 * the service name and the value is a provider for the network service instance.
 */
abstract class VkServiceExtension(
    /**
     * A provider of a map containing VK network service providers,
     * keyed by service name.
     */
    val services: Provider<Map<String, Provider<VkService>>>,
)
