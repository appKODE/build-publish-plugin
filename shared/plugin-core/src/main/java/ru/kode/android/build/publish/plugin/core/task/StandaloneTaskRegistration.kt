package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.logger.LoggerService

/**
 * Registers a [StandaloneServiceTask] and wires its shared [service]/[loggerService] inputs plus the
 * matching `usesService` declarations. Centralizes the identical four-line boilerplate every
 * standalone task registration used to repeat.
 *
 * @param name the task name (see [TaskNames])
 * @param service the build service backing the task
 * @param loggerService the shared logger service
 * @param configure additional task-specific configuration
 */
inline fun <reified T : StandaloneServiceTask<S>, S : BuildService<*>> Project.registerStandaloneServiceTask(
    name: String,
    service: Provider<S>,
    loggerService: Provider<LoggerService>,
    crossinline configure: T.() -> Unit = {},
): TaskProvider<T> =
    tasks.register(name, T::class.java) { task ->
        task.service.set(service)
        task.loggerService.set(loggerService)
        task.usesService(service)
        task.usesService(loggerService)
        task.configure()
    }
