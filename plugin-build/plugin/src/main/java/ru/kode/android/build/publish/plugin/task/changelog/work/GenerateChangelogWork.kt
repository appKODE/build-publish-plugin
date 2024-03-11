package ru.kode.android.build.publish.plugin.task.changelog.work

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.changelog.git.ChangelogBuilder
import ru.kode.android.build.publish.plugin.task.changelog.git.GitRepository
import javax.inject.Inject

interface GenerateChangelogParameters : WorkParameters {
    val commitMessageKey: Property<String>
    val buildTagPattern: Property<String>
    val tagBuildFile: RegularFileProperty
    val changelogFile: RegularFileProperty
    val grgitService: Property<GrgitService>
}

abstract class GenerateChangelogWork
    @Inject
    constructor() : WorkAction<GenerateChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val messageKey = parameters.commitMessageKey.get()
            val buildTagPattern = parameters.buildTagPattern.orNull
            val currentBuildTag = fromJson(parameters.tagBuildFile.asFile.get())
            val gitCommandExecutor = GitCommandExecutor(parameters.grgitService.get())
            val gitRepository = GitRepository(gitCommandExecutor)
            val changelog =
                ChangelogBuilder(gitRepository, gitCommandExecutor, logger, messageKey)
                    .buildForBuildTag(
                        currentBuildTag,
                        buildTagPattern,
                        defaultValueSupplier = { tagRange ->
                            val previousBuildName = tagRange.previousBuildTag?.name?.let { "($it)" }
                            "No changes compared to the previous build $previousBuildName"
                        },
                    )
            val changelogOutput = parameters.changelogFile.asFile.get()
            if (changelog.isNullOrBlank()) {
                logger.info("changelog not generated")
            } else {
                logger.info("generate changelog")
                changelogOutput.writeText(changelog)
            }
        }
    }
