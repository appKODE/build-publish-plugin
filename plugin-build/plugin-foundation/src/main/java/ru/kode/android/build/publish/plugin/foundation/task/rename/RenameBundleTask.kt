package ru.kode.android.build.publish.plugin.foundation.task.rename

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.foundation.messages.renameBundleMessage
import java.io.File
import javax.inject.Inject

/**
 * A task for renaming an AAB (Bundle) file.
 *
 * This task is wired as an Android Gradle Plugin artifact transform. It takes the produced Bundle
 * from [inputFile] and copies it into [outputFile]. It also creates a copy with the name
 * provided via [outputFileName] in the same directory to ensure the renamed artifact is present
 * in the build outputs.
 */
@CacheableTask
abstract class RenameBundleTask
    @Inject
    constructor() : DefaultTask() {
        /**
         * The logger service property.
         *
         * This service is used for logging debug and error messages.
         *
         * @see LoggerService
         */
        @get:Internal
        abstract val loggerService: Property<LoggerService>

        /**
         * The name of the output Bundle file.
         *
         * The task will rename the input Bundle file to this name.
         */
        @get:Input
        abstract val outputFileName: Property<String>

        /**
         * The input Bundle file.
         *
         * This is a file produced by AGP that contains the built Bundle artifact.
         */
        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val inputFile: RegularFileProperty

        /**
         * The output Bundle file.
         *
         * This is the output file of the artifact transform.
         */
        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        /**
         * Renames the Bundle file.
         *
         * Copies the input Bundle file to the output directory with the specified name.
         */
        @TaskAction
        fun rename() {
            val input = inputFile.get().asFile
            val output = outputFile.get().asFile

            println("OUTPUT BUNDLE FILE: $output")

            val targetName = outputFileName.get()
            val outputDir = output.parentFile

            loggerService.get().info(renameBundleMessage(input, targetName, outputDir))

            // Fulfill the transform
            input.copyTo(output, overwrite = true)

            // Also copy to the target name if it differs from the AGP-provided output name
            val renamedFile = File(outputDir, targetName)
            println("RENAMED BUNDLE FILE: $renamedFile")
            if (renamedFile.absolutePath != output.absolutePath) {
                input.copyTo(renamedFile, overwrite = true)
            }
        }
    }
