package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.util.Changelog

abstract class GenerateChangelogTask : DefaultTask() {

    init {
        description = "Generate changelog task"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = getCommandExecutor(project)

    @get:Input
    @get:Option(option = "buildVariant", description = "Current build variant")
    abstract val buildVariant: Property<String>

    @get:Input
    @get:Option(
        option = "commitMessageKey",
        description = "Message key to collect interested commits"
    )
    abstract val commitMessageKey: Property<String>

    @get:OutputFile
    @get:Option(
        option = "changelogFile",
        description = "File to store changelog"
    )
    abstract val changelogFile: RegularFileProperty

    @TaskAction
    fun generateChangelog() {
        val messageKey = commitMessageKey.get()
        val changelog = Changelog(commandExecutor, project.logger, messageKey, setOf(buildVariant.get()))
            .buildForRecentBuildTag(
                defaultValueSupplier = { tagRange ->
                    val previousBuildName = tagRange.previousBuildTag?.name?.let { "(**$it**)" }
                    "No changes in comparison with a previous build $previousBuildName"
                }
            )
        project.logger.debug("generated changelog:")
        project.logger.debug(changelog)
        val output = changelogFile.get().asFile
        output.writeText(changelog.orEmpty())
    }
}
