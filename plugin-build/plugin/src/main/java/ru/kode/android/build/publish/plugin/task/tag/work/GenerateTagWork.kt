package ru.kode.android.build.publish.plugin.task.tag.work

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.enity.mapper.toJson
import ru.kode.android.build.publish.plugin.task.changelog.git.GitRepository
import javax.inject.Inject

interface GenerateTagParameters : WorkParameters {
    val buildVariant: Property<String>
    val tagBuildFile: RegularFileProperty
    val grgitService: Property<GrgitService>
}

abstract class GenerateTagWork @Inject constructor() : WorkAction<GenerateTagParameters> {

    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val buildVariants = setOf(parameters.buildVariant.get())
        val gitCommandExecutor = GitCommandExecutor(parameters.grgitService.get())
        val buildTag = GitRepository(gitCommandExecutor, buildVariants).findRecentBuildTag()
        val tagBuildOutput = parameters.tagBuildFile.asFile.get()

        if (buildTag != null) {
            logger.debug("last tag ${buildTag.name}, build number ${buildTag.buildNumber}")
            tagBuildOutput.writeText(buildTag.toJson())
        } else {
            logger.debug("build tag not created")
        }
    }
}
