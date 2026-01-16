package ru.kode.android.build.publish.plugin.core.logger

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import javax.inject.Inject

const val LOGGER_SERVICE_NAME = "loggerService"
const val LOGGER_SERVICE_EXTENSION_NAME = "loggerServiceExtension"

/**
 * A build service that provides a logger for plugins.
 *
 * This logger service acts as a central point for logging debug and error messages during the build process.
 * It abstracts the underlying logging mechanism and allows plugins to log messages without caring about the
 * specific logging implementation.
 */
abstract class LoggerService
    @Inject
    constructor(
        providerFactory: ProviderFactory,
    ) : BuildService<LoggerService.Params> {
        interface Params : BuildServiceParameters {
            val verboseLogging: Property<Boolean>
            val bodyLogging: Property<Boolean>
        }

        private val loggerProvider: Provider<PluginLogger> =
            parameters.bodyLogging.flatMap { bodyLogging ->
                providerFactory.provider { Logging.getLogger("BuildPublish") }
                    .zip(parameters.verboseLogging) { logger, verboseLogging ->
                        DefaultPluginLogger(logger, verboseLogging, bodyLogging)
                    }
            }

        val logger: PluginLogger get() = loggerProvider.get()

        fun info(
            message: String,
            exception: Throwable? = null,
        ) {
            logger.info(message, exception)
        }

        fun warn(message: String) {
            logger.warn(message)
        }

        fun error(
            message: String,
            exception: Throwable? = null,
        ) {
            logger.error(message, exception)
        }

        fun quiet(message: String) {
            logger.quiet(message)
        }
    }

/**
 * Extension for the [LoggerService].
 *
 * This extension provides access to the [LoggerService] for plugins.
 */
abstract class LoggerServiceExtension(
    /**
     * Provider for the [LoggerService].
     *
     * This provider provides access to the [LoggerService] for plugins.
     */
    val service: Provider<LoggerService>,
)
