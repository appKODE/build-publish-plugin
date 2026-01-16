package ru.kode.android.build.publish.plugin.core.logger

import org.gradle.api.logging.Logger

/**
 * Interface for logging plugin-related messages.
 *
 * Allows different implementations to be used for logging, such as Gradle's logger or a custom logger.
 */
interface PluginLogger {
    val bodyLogging: Boolean

    /**
     * Logs an informational message.
     *
     * @param message The message to log.
     * @param exception An optional exception to log.
     */
    fun info(
        message: String,
        exception: Throwable? = null,
    )

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    fun warn(message: String)

    /**
     * Logs an error message.
     *
     * @param message The message to log.
     * @param exception An optional exception to log.
     */
    fun error(
        message: String,
        exception: Throwable? = null,
    )

    /**
     * Logs a message without raising the log level.
     *
     * @param message The message to log.
     */
    fun quiet(message: String)
}

/**
 * Default implementation of [PluginLogger] that wraps a [Logger].
 */
class DefaultPluginLogger(
    /**
     * The underlying [Logger] that is being wrapped.
     */
    private val logger: Logger,
    /**
     * Indicates whether verbose logging is enabled.
     *
     * Verbose logging includes additional log messages that provide more detailed information.
     */
    private val verboseLogging: Boolean,
    /**
     * Indicates whether body logging is enabled.
     *
     * Body logging includes additional log messages that provide more detailed information,
     * such as the contents of HTTP requests and responses.
     */
    override val bodyLogging: Boolean,
) : PluginLogger {
    override fun info(
        message: String,
        exception: Throwable?,
    ) {
        if (verboseLogging) {
            logger.lifecycle(message, exception)
        } else {
            logger.info(message, exception)
        }
    }

    override fun warn(message: String) {
        if (verboseLogging) {
            logger.lifecycle(message)
        } else {
            logger.warn(message)
        }
    }

    override fun error(
        message: String,
        exception: Throwable?,
    ) {
        if (verboseLogging) {
            logger.lifecycle(message)
        } else {
            logger.error(message, exception)
        }
    }

    override fun quiet(message: String) {
        logger.quiet(message)
    }
}
