package ru.kode.android.build.publish.plugin.foundation.task.tag.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.service.GitExecutorService
import javax.inject.Inject

interface GenerateTagParameters : WorkParameters {
    val buildVariant: Property<String>
    val buildTagPattern: Property<String>
    val tagBuildFile: RegularFileProperty
    val gitExecutorService: Property<GitExecutorService>
}

abstract class GenerateTagWork
    @Inject
    constructor() : WorkAction<GenerateTagParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val buildVariant = parameters.buildVariant.get()
            val buildTagPattern = parameters.buildTagPattern.orNull
            val buildTag = parameters.gitExecutorService.get()
                .repository
                .findRecentBuildTag(buildVariant, buildTagPattern)
            val tagBuildOutput = parameters.tagBuildFile.asFile.get()

            if (buildTag != null) {
                logger.info("last tag ${buildTag.name}, build number ${buildTag.buildNumber}")
                tagBuildOutput.writeText(buildTag.toJson())
            } else {
                logger.info("build tag not created")
            }
        }
    }
