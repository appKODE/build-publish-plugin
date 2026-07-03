package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService

interface ServiceWorkParameters : WorkParameters {
    val loggerService: Property<LoggerService>
}
