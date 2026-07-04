@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.core.util

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.logger.LOGGER_SERVICE_NAME
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension

fun Project.getOrRegisterLoggerService(): Provider<LoggerService> =
    extensions.findByType(LoggerServiceExtension::class.java)?.service
        ?: gradle.sharedServices.registerIfAbsent(
            serviceName(LOGGER_SERVICE_NAME),
            LoggerService::class.java,
        ) {
            it.parameters.verboseLogging.set(false)
            it.parameters.bodyLogging.set(false)
        }

fun Project.applyWithOptionalAndroid(block: () -> Unit) {
    val android = extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
    if (android != null) android.finalizeDsl { block() } else afterEvaluate { block() }
}
