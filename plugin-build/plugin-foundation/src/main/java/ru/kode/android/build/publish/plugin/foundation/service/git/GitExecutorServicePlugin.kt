package ru.kode.android.build.publish.plugin.foundation.service.git

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.util.serviceName

internal const val GOT_EXECUTOR_SERVICE_NAME = "gitExecutorService"
internal const val GIT_EXECUTOR_SERVICE_NAME_EXTENSION = "gitExecutorServiceExtension"

/**
 * A Gradle plugin that provides Git operations through a shared service.
 *
 * This plugin:
 * - Applies the GrgitServicePlugin for Git repository access
 * - Registers a shared [GitExecutorService] for performing Git operations
 * - Provides access to the service through a project extension
 *
 * The service is registered as a shared service to ensure thread-safe Git operations
 * across different tasks and projects.
 */
abstract class GitExecutorServicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val grGitService =
            project.extensions
                .getByType(GrgitServiceExtension::class.java)
                .service

        val loggerProvider =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        val serviceProvider =
            project.gradle.sharedServices.registerIfAbsent(
                project.serviceName(GOT_EXECUTOR_SERVICE_NAME),
                GitExecutorService::class.java,
                {
                    it.maxParallelUsages.set(1)
                    it.parameters.grgitService.set(grGitService)
                    it.parameters.loggerService.set(loggerProvider)
                },
            )

        project.extensions.create(
            GIT_EXECUTOR_SERVICE_NAME_EXTENSION,
            GitExecutorServiceExtension::class.java,
            serviceProvider,
        )
    }
}
