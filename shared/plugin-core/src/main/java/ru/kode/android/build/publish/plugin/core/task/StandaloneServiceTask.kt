package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import javax.inject.Inject

@DisableCachingByDefault
abstract class StandaloneServiceTask<S : BuildService<*>>
    @Inject
    constructor(
        @get:Internal protected val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val service: Property<S>

        @get:Internal
        abstract val loggerService: Property<LoggerService>
    }
