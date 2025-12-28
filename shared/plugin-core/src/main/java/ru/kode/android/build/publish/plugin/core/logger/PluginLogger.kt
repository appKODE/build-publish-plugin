package ru.kode.android.build.publish.plugin.core.logger

import org.gradle.api.logging.Logger

interface PluginLogger {
    val bodyLogging: Boolean

    fun info(
        message: String,
        exception: Throwable? = null,
    )

    fun warn(message: String)

    fun error(
        message: String,
        exception: Throwable? = null,
    )

    fun quiet(message: String)
}

class DefaultPluginLogger(
    private val logger: Logger,
    private val verboseLogging: Boolean,
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
        if (verboseLogging) {
            logger.lifecycle(message)
        } else {
            logger.quiet(message)
        }
    }
}
