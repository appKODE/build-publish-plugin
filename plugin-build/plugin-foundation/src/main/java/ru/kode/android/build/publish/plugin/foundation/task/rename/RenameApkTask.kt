package ru.kode.android.build.publish.plugin.foundation.task.rename

import com.android.build.api.artifact.ArtifactTransformationRequest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.foundation.messages.renameApkMessage
import java.io.File
import javax.inject.Inject

/**
 * A task for renaming an APK file.
 *
 * This task is wired as an Android Gradle Plugin artifact transform. It takes the produced APK
 * (from [inputDir]) and copies it into [outputDir] with the name provided via [outputFileName].
 */
abstract class RenameApkTask
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
         * The name of the output APK file.
         *
         * The task will rename the input APK file to this name.
         */
        @get:Input
        abstract val outputFileName: Property<String>

        /**
         * The directory containing the input APK file.
         *
         * This is a directory produced by AGP that contains the built APK artifact.
         */
        @get:InputDirectory
        abstract val inputDir: DirectoryProperty

        /**
         * The directory to which the renamed APK file will be written.
         *
         * This is the output directory of the artifact transform.
         */
        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        /**
         * The transformation request for renaming the APK file.
         *
         * This property is used to store the transformation request for renaming the APK file.
         * The transformation request is used to submit the transformation logic to the task
         * execution engine.
         *
         * The transformation request is of type [ArtifactTransformationRequest], which is a
         * property wrapper for [ArtifactTransformationRequest] that provides Gradle property
         * behavior.
         *
         * @see ArtifactTransformationRequest
         */
        @get:Internal
        abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApkTask>>

        /**
         * Renames the APK file.
         *
         * Copies the input APK file to the output directory with the specified name.
         */
        @TaskAction
        fun rename() {
            val request = transformationRequest.get()
            request.submit(this) { builtArtifact ->
                val inputFile = File(builtArtifact.outputFile)
                val outputDir = outputDir.get().asFile
                val targetOutputFileName = outputFileName.get()
                val outputFile = File(outputDir, targetOutputFileName)
                loggerService.get().info(renameApkMessage(inputFile, targetOutputFileName, outputDir))
                inputFile.copyTo(outputFile, overwrite = true)
                outputFile
            }
        }
    }
