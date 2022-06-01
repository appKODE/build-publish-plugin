package ru.kode.android.build.publish.plugin.task

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.util.ChangelogBuilder

/**
 * Generate changelog task should write to file,
 * then result can be used in Firebase App Distribution without rebuilding configs
 * and it can be used in all other tasks without duplicates in different places
 */
abstract class GenerateChangelogTask : DefaultTask() {

    init {
        description = "Generate changelog task"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = getCommandExecutor(project)

    @get:InputFile
    @get:Optional
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

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
        val tagBuildFile = tagBuildFile.asFile.get()
        val currentBuildTag = fromJson(tagBuildFile)
        val gitRepository = GitRepository(commandExecutor, setOf(buildVariant.get()))
        val changelog = ChangelogBuilder(gitRepository, commandExecutor, logger, messageKey)
            .buildForBuildTag(
                currentBuildTag,
                defaultValueSupplier = { tagRange ->
                    val previousBuildName = tagRange.previousBuildTag?.name?.let { "(**$it**)" }
                    "No changes in comparison with a previous build $previousBuildName"
                }
            )
        val changelogOutput = changelogFile.get().asFile
        if (!changelog.isNullOrBlank()) {
            changelogOutput.writeText(changelog)
        }
    }

    override fun doLast(action: Action<in Task>): Task {
        return super.doLast(action)
    }
}
