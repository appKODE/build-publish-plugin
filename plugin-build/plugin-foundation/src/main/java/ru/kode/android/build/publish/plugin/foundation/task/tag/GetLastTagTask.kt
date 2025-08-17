package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import ru.kode.android.build.publish.plugin.foundation.task.tag.work.GenerateTagWork
import javax.inject.Inject

/**
 * A Gradle task that retrieves the last Git tag matching a specific pattern for a build variant.
 *
 * This task is responsible for:
 * - Finding the most recent Git tag that matches the specified pattern
 * - Handling different build variants
 * - Supporting fallback to stub values when no matching tag is found
 * - Writing the tag information to a JSON file
 *
 * @see DefaultTask
 * @see GenerateTagWork
 */
@DisableCachingByDefault
abstract class GetLastTagTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Retrieves the last Git tag matching a specific pattern for a build variant"
            group = BasePlugin.BUILD_GROUP
            outputs.upToDateWhen { false }
        }

        /**
         * The Git executor service used to interact with the Git repository.
         *
         * This service provides Git operations needed to find and process tags.
         *
         * @see GitExecutorService
         */
        @get:ServiceReference
        abstract val gitExecutorService: Property<GitExecutorService>

        /**
         * The name of the current build variant.
         *
         * This is used to filter tags specific to the current build variant.
         * Example values: "debug", "release", "staging"
         */
        @get:Input
        @get:Option(
            option = "buildVariantName",
            description = "Current build variant name (e.g., 'debug', 'release')",
        )
        abstract val buildVariantName: Property<String>

        /**
         * The pattern used to filter Git tags.
         *
         * This pattern is used with `git describe --match` to find relevant tags.
         */
        @get:Input
        @get:Option(
            option = "buildTagPattern",
            description = "Pattern to match Git tags",
        )
        abstract val buildTagPattern: Property<String>

        /**
         * Whether to use stub values when no matching tag is found.
         *
         * When set to `true`, if no matching tag is found, the task will use default values
         * instead of failing. This is useful for new projects or branches without tags.
         */
        @get:Input
        @get:Option(
            option = "useStubsForTagAsFallback",
            description = "Use default values if no matching tag is found (prevents build failures)",
        )
        abstract val useStubsForTagAsFallback: Property<Boolean>

        /**
         * The output file where tag information will be written in JSON format.
         *
         * The JSON file will contain information about the found tag.
         */
        @get:OutputFile
        @get:Option(
            option = "tagBuildFile",
            description = "Output JSON file containing information about the found build tag",
        )
        abstract val tagBuildFile: RegularFileProperty

        /**
         * Executes the task to find and process the last Git tag.
         *
         * This method:
         * 1. Creates a work queue for background processing
         * 2. Submits a [GenerateTagWork] task with the configured parameters
         * 3. Waits for the work to complete
         *
         * The actual tag processing is done in a separate worker thread to avoid
         * blocking the main Gradle build thread during Git operations.
         *
         * @see GenerateTagWork
         */
        @TaskAction
        fun getLastTag() {
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(GenerateTagWork::class.java) { parameters ->
                parameters.tagBuildFile.set(tagBuildFile)
                parameters.buildVariant.set(buildVariantName)
                parameters.buildTagPattern.set(buildTagPattern)
                parameters.gitExecutorService.set(gitExecutorService)
                parameters.useStubsForTagAsFallback.set(useStubsForTagAsFallback)
            }
            workQueue.await()
        }
    }
