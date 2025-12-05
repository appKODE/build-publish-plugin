package ru.kode.android.build.publish.plugin.foundation.task.rename

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

abstract class RenameApkTask : DefaultTask() {
    private val logger: Logger = Logging.getLogger(this::class.java)

    @get:InputFile
    abstract val inputApkFile: RegularFileProperty

    @get:OutputFile
    abstract val renamedApkFile: RegularFileProperty

    @TaskAction
    fun renameApk() {
        val src = inputApkFile.get().asFile
        val dest = renamedApkFile.get().asFile

        dest.parentFile.mkdirs()
        Files.copy(
            src.toPath(),
            dest.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        )
        logger.info("Renamed apk from $src to $dest")
    }
}