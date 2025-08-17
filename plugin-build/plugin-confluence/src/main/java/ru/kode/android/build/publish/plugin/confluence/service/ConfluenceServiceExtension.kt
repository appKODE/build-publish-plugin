package ru.kode.android.build.publish.plugin.confluence.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.confluence.service.network.ConfluenceNetworkService

/**
 * Extension for configuring and accessing Confluence services within the build.
 *
 * This class provides access to configured Confluence network services that can be used
 * to interact with Confluence's API. The services are provided as a map where the key is
 * the service name and the value is a provider for the network service instance.
 */
abstract class ConfluenceServiceExtension(
    /**
     * A provider of a map containing Confluence network service providers,
     * keyed by service name.
     */
    val networkServices: Provider<Map<String, Provider<ConfluenceNetworkService>>>,
)
