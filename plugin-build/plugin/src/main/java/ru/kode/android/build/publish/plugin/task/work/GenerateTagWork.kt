package ru.kode.android.build.publish.plugin.task.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.mapper.toJson
import javax.inject.Inject

interface GenerateTagParameters : WorkParameters {
    val buildVariant: Property<String>
    val tagBuildFile: RegularFileProperty
}

abstract class GenerateTagWork @Inject constructor(
    execOperations: ExecOperations,
) : WorkAction<GenerateTagParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val commandExecutor = getCommandExecutor(execOperations)

    override fun execute() {
        val buildVariants = setOf(parameters.buildVariant.get())
        val buildTag = GitRepository(commandExecutor, buildVariants).findRecentBuildTag()
        val tagBuildOutput = parameters.tagBuildFile.asFile.get()

        if (buildTag != null) {
            logger.debug("last tag ${buildTag.name}, build number ${buildTag.buildNumber}")
            tagBuildOutput.writeText(buildTag.toJson())
        } else {
            logger.debug("build tag not created")
        }
    }
}
