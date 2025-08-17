package ru.kode.android.build.publish.plugin.foundation.service.git

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.util.serviceName

internal const val SERVICE_NAME = "gitExecutorService"
internal const val SERVICE_NAME_EXTENSION = "gitExecutorServiceExtension"

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

        val service =
            project.gradle.sharedServices.registerIfAbsent(
                project.serviceName(SERVICE_NAME),
                GitExecutorService::class.java,
                {
                    it.maxParallelUsages.set(1)
                    it.parameters.grgitService.set(grGitService)
                },
            )

        project.extensions.create(
            SERVICE_NAME_EXTENSION,
            GitExecutorServiceExtension::class.java,
            service,
        )
    }
}
