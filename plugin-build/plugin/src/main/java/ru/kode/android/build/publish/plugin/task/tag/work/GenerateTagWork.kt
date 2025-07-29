package ru.kode.android.build.publish.plugin.task.tag.work

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.core.mapper.toJson
import ru.kode.android.build.publish.plugin.task.changelog.git.GitRepository
import javax.inject.Inject

interface GenerateTagParameters : WorkParameters {
    val buildVariant: Property<String>
    val buildTagPattern: Property<String>
    val tagBuildFile: RegularFileProperty
    val grgitService: Property<GrgitService>
}

abstract class GenerateTagWork
    @Inject
    constructor() : WorkAction<GenerateTagParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val buildVariant = parameters.buildVariant.get()
            val buildTagPattern = parameters.buildTagPattern.orNull
            val gitCommandExecutor = GitCommandExecutor(parameters.grgitService.get().grgit)
            val buildTag = GitRepository(gitCommandExecutor).findRecentBuildTag(buildVariant, buildTagPattern)
            val tagBuildOutput = parameters.tagBuildFile.asFile.get()

            if (buildTag != null) {
                logger.info("last tag ${buildTag.name}, build number ${buildTag.buildNumber}")
                tagBuildOutput.writeText(buildTag.toJson())
            } else {
                logger.info("build tag not created")
            }
        }
    }
