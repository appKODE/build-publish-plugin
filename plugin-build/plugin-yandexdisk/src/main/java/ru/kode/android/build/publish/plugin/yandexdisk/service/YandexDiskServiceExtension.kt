package ru.kode.android.build.publish.plugin.yandexdisk.service

import org.gradle.api.provider.Provider

/**
 * Extension for configuring and accessing Yandex Disk services within the build.
 *
 * This class provides access to configured Yandex Disk network services that can be used
 * to interact with Yandex Disk's API. The services are provided as a map where the key is
 * the service name and the value is a provider for the network service instance.
 */
abstract class YandexDiskServiceExtension(
    /**
     * A provider of a map containing Yandex Disk network service providers,
     * keyed by service name.
     */
    val services: Provider<Map<String, Provider<YandexDiskService>>>,
)
