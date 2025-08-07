package ru.kode.android.build.publish.plugin.foundation.task.changelog.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.mapper.fromJson
import ru.kode.android.build.publish.plugin.foundation.service.GitExecutorService
import javax.inject.Inject

interface GenerateChangelogParameters : WorkParameters {
    val commitMessageKey: Property<String>
    val buildTagPattern: Property<String>
    val tagBuildFile: RegularFileProperty
    val changelogFile: RegularFileProperty
    val gitExecutorService: Property<GitExecutorService>
}

abstract class GenerateChangelogWork
    @Inject
    constructor() : WorkAction<GenerateChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val messageKey = parameters.commitMessageKey.get()
            val buildTagPattern = parameters.buildTagPattern.get()
            val currentBuildTag = fromJson(parameters.tagBuildFile.asFile.get())
            val changelog =
                parameters.gitExecutorService.get()
                    .changelogBuilder
                    .buildForBuildTag(
                        messageKey,
                        currentBuildTag,
                        buildTagPattern,
                        defaultValueSupplier = { tagRange ->
                            val previousBuildName = tagRange.previousBuildTag?.name?.let { "($it)" }
                            "No changes compared to the previous build $previousBuildName"
                        },
                    )
            val changelogOutput = parameters.changelogFile.asFile.get()
            if (changelog.isNullOrBlank()) {
                logger.info(
                    "changelog is NOT generated for `$buildTagPattern` buildTagPattern " +
                        "and `$currentBuildTag` build tag",
                )
                changelogOutput.writeText(
                    "No changes because changelog is not generated for tag ${currentBuildTag.name}",
                )
            } else {
                logger.info(
                    "changelog is generated for `$buildTagPattern` buildTagPattern " +
                        "and `$currentBuildTag` build tag",
                )
                changelogOutput.writeText(changelog)
            }
        }
    }
