package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option

abstract class GetLastTagSnapshotTaskOutput : DefaultTask() {
    /**
     * The output file where tag information will be written in JSON format.
     *
     * The JSON file will contain information about the found tag.
     */
    @get:OutputFile
    @get:Option(
        option = "buildTagSnapshotFile",
        description = "Output JSON file containing information about the found build tag",
    )
    abstract val buildTagSnapshotFile: RegularFileProperty
}
