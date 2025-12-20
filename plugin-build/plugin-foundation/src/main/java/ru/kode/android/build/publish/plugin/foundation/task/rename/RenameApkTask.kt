package ru.kode.android.build.publish.plugin.foundation.task.rename

import com.android.build.api.artifact.ArtifactTransformationRequest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import ru.kode.android.build.publish.plugin.foundation.messages.renameApkMessage
import java.io.File
import javax.inject.Inject

/**
 * A task for renaming an APK file.
 *
 * This task is used to rename an APK file to a specified name. The task takes the input APK file
 * and renames it to the specified name in the output directory.
 */
abstract class RenameApkTask
    @Inject
    constructor() : DefaultTask() {
        private val logger: Logger = Logging.getLogger(this::class.java)

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
         * The task will rename the file with the name specified in [outputFileName]
         * in this directory.
         */
        @get:InputDirectory
        abstract val inputDir: DirectoryProperty

        /**
         * The directory to which the renamed APK file will be written.
         *
         * The task will rename the file with the name specified in [outputFileName]
         * in this directory.
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
                logger.info(renameApkMessage(inputFile, targetOutputFileName, outputDir))
                inputFile.copyTo(outputFile, overwrite = true)
                outputFile
            }
        }
    }
