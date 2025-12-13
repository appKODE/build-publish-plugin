package ru.kode.android.build.publish.plugin.confluence.controller.factory

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceController
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceControllerImpl
import ru.kode.android.build.publish.plugin.confluence.network.factory.ConfluenceApiFactory
import ru.kode.android.build.publish.plugin.confluence.network.factory.ConfluenceClientFactory
import ru.kode.android.build.publish.plugin.core.util.NetworkProxy

/**
 * Factory for creating instances of [ConfluenceController].
 */
object ConfluenceControllerFactory {

    /**
     * Creates an instance of [ConfluenceController].
     *
     * @param baseUrl The base URL of the Confluence instance.
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @param logger The logger to use for logging.
     * @param proxy A factory function to create an instance of [NetworkProxy] if needed.
     * @return An instance of [ConfluenceController].
     */
    fun build(
        baseUrl: String,
        username: String,
        password: String,
        logger: Logger,
        proxy: () -> NetworkProxy
    ): ConfluenceController {
        return ConfluenceControllerImpl(
            baseUrl = baseUrl,
            api = ConfluenceApiFactory.build(
                client = ConfluenceClientFactory.build(
                    username,
                    password,
                    logger,
                    proxy
                ),
                baseUrl = baseUrl
            ),
            logger = logger
        )
    }
}
