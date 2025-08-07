package ru.kode.android.build.publish.plugin.foundation.service

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

internal const val GIT_EXECUTOR_SERVICE_NAME = "gitExecutorService"

abstract class GitExecutorServicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val grGitService =
            project.extensions
                .getByType(GrgitServiceExtension::class.java)
                .service

        project.gradle.sharedServices.registerIfAbsent(
            "${GIT_EXECUTOR_SERVICE_NAME}_{${project.name}}",
            GitExecutorService::class.java,
            {
                it.maxParallelUsages.set(1)
                it.parameters.grgitService.set(grGitService)
            },
        )
    }
}
