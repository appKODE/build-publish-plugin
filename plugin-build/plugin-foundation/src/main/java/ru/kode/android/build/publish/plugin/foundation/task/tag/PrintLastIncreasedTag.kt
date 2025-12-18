package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.util.replaceLast

/**
 * A Gradle task that prints the next incremented build tag based on the last build tag.
 *
 * This task reads the last build tag from a JSON file, increments the build number,
 * and prints the new tag to standard output. It's typically used in CI/CD pipelines
 * to determine the next version tag for a build.
 *
 * ## Usage
 * ```
 * ./gradlew printLastIncreasedTag --buildTagFile=path/to/build-tag.json
 * ```
 *
 * @see DefaultTask
 */
@DisableCachingByDefault
abstract class PrintLastIncreasedTag : DefaultTask() {
    init {
        description = "Task to print last increased tag"
        group = BasePlugin.BUILD_GROUP
    }

    /**
     * The JSON file containing information about the last build tag.
     *
     * This file should contain a JSON object with the following structure:
     * The task will increment the build number and print the new tag name.
     *
     * @see RegularFileProperty
     */
    @get:InputFile
    @get:Option(
        option = "buildTagFile",
        description = "JSON file containing information about the last build tag",
    )
    abstract val buildTagFile: RegularFileProperty

    /**
     * Executes the task to print the next incremented build tag.
     *
     * This method:
     * 1. Reads the last build tag from the JSON file
     * 2. Increments the build number
     * 3. Generates the next tag by replacing the build number in the tag name
     * 4. Prints the new tag to standard output
     *
     * @throws Exception if there's an error reading the build tag file or processing the tag
     */
    @TaskAction
    fun printTag() {
        val lastBuildTag = fromJson(buildTagFile.asFile.get())
        val currentBuildNumber = lastBuildTag.buildNumber.toString()
        val increasedBuildNumber = lastBuildTag.buildNumber.inc().toString()
        val nextBuildTag = lastBuildTag.name
            .replaceLast(currentBuildNumber, increasedBuildNumber)
        print(nextBuildTag)
    }
}
