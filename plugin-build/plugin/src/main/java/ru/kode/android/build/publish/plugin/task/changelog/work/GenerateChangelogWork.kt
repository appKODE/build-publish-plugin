package ru.kode.android.build.publish.plugin.task.changelog.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.task.changelog.git.GitRepository
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.changelog.git.ChangelogBuilder
import ru.kode.android.build.publish.plugin.util.ellipsizeAt
import javax.inject.Inject

interface GenerateChangelogParameters : WorkParameters {
    val commitMessageKey: Property<String>
    val buildVariant: Property<String>
    val tagBuildFile: RegularFileProperty
    val changelogFile: RegularFileProperty
}

abstract class GenerateChangelogWork @Inject constructor(
    execOperations: ExecOperations,
) : WorkAction<GenerateChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val commandExecutor = getCommandExecutor(execOperations)

    override fun execute() {
        val messageKey = parameters.commitMessageKey.get()
        val currentBuildTag = fromJson(parameters.tagBuildFile.asFile.get())
        val buildVariants = setOf(parameters.buildVariant.get())
        val gitRepository = GitRepository(commandExecutor, buildVariants)
        val changelog = ChangelogBuilder(gitRepository, commandExecutor, logger, messageKey)
            .buildForBuildTag(
                currentBuildTag,
                defaultValueSupplier = { tagRange ->
                    val previousBuildName = tagRange.previousBuildTag?.name?.let { "(**$it**)" }
                    "No changes in comparison with a previous build $previousBuildName"
                }
            )
            ?.ellipsizeAt(MAX_CHANGELOG_SYMBOLS)
        val changelogOutput = parameters.changelogFile.asFile.get()
        if (changelog.isNullOrBlank()) {
            logger.debug("changelog not generated")
        } else {
            logger.debug("generate changelog")
            changelogOutput.writeText(changelog)
        }
    }
}

private const val MAX_CHANGELOG_SYMBOLS = 2000
