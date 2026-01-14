package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option

abstract class GenerateChangelogTaskOutput : DefaultTask() {
    @get:OutputFile
    @get:Option(
        option = "changelogFile",
        description = "The output file where the generated changelog will be saved",
    )
    abstract val changelogFile: RegularFileProperty
}
