package ru.kode.android.build.publish.plugin.foundation.task.tag.work

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_BUILD_VERSION
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import javax.inject.Inject

internal interface GenerateTagParameters : WorkParameters {
    val buildVariant: Property<String>
    val buildTagPattern: Property<String>
    val tagBuildFile: RegularFileProperty
    val gitExecutorService: Property<GitExecutorService>
    val useStubsForTagAsFallback: Property<Boolean>
}

internal abstract class GenerateTagWork
    @Inject
    constructor() : WorkAction<GenerateTagParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val buildVariant = parameters.buildVariant.get()
            val buildTagPattern = parameters.buildTagPattern.get()
            val buildTag =
                parameters.gitExecutorService.get()
                    .repository
                    .findRecentBuildTag(buildVariant, buildTagPattern)
            val tagBuildOutput = parameters.tagBuildFile.asFile.get()
            val useStubsForTagAsFallback = parameters.useStubsForTagAsFallback.get()

            if (buildTag != null) {
                logger.info(
                    "last tag ${buildTag.name}, build number ${buildTag.buildNumber} was found, " +
                        "and tag build file is generated",
                )
                tagBuildOutput.writeText(buildTag.toJson())
            } else if (useStubsForTagAsFallback) {
                tagBuildOutput.writeText(
                    Tag.Build(
                        name = buildTagPattern,
                        commitSha = "STUB COMMIT SHA",
                        message = "WARNING: Not real tag, not use it for release",
                        buildVersion = DEFAULT_BUILD_VERSION,
                        buildVariant = buildVariant,
                        buildNumber = DEFAULT_VERSION_CODE,
                    ).toJson(),
                )
            } else {
                logger.info(
                    "build tag file not created for '$buildVariant' build variant. " +
                        "Maybe pattern is wrong, or tag not exists, or tag was not fetched",
                )
                throw GradleException(
                    "There is no last tag for '$buildVariant' build variant which " +
                        "matches `$buildTagPattern` pattern. \n" +
                        "It's a crucial file for all other tasks. without it nothing will work. \n" +
                        "Check that tag for that build variant exists, if not - create it. \n" +
                        "If it exists, try to rerun task with --info property and debug it",
                )
            }
        }
    }
