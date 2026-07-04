package ru.kode.android.build.publish.plugin.sender.task.base

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@DisableCachingByDefault
abstract class BaseNextcloudSenderTask
    @Inject
    constructor(
        @get:Internal protected val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val baseUrl: Property<String>

        @get:Internal
        abstract val username: Property<String>

        @get:Internal
        abstract val password: Property<String>
    }
