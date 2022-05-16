package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.LinuxShellCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.entity.Tag

abstract class PrintLastIncreasedTag : DefaultTask() {

    init {
        description = "Task to print last increased tag"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = LinuxShellCommandExecutor(project)

    @get:Input
    @get:Option(option = "buildVariants", description = "List of all available build variants")
    abstract val buildVariants: SetProperty<String>

    @get:Input
    @get:Optional
    @get:Option(option = "variant", description = "Priority variant")
    abstract val variant: Property<String>

    @TaskAction
    fun printTag() {
        val buildVariants = variant.orNull?.let { setOf(it) } ?: buildVariants.get()
        val buildTag = getBuildTag(buildVariants)
        val currentBuildNumber = buildTag.buildNumber.toString()
        val increasedBuildNumber = buildTag.buildNumber.inc().toString()
        val nextTag = buildTag.name.replaceFirst(currentBuildNumber, increasedBuildNumber)
        print(nextTag)
    }

    private fun getBuildTag(buildVariants: Set<String>): Tag.Build {
        return GitRepository(commandExecutor, buildVariants)
            .findMostRecentBuildTag()
            ?: throw GradleException("unable to send changelog: failed to find most recent build tag")
    }
}
