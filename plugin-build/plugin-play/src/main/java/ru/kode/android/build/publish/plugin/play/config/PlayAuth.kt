package ru.kode.android.build.publish.plugin.play.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

interface PlayAuth {
    val name: String

    /**
     * The path to file with token for Google Play project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * appId in Google Play
     */
    @get:Input
    val appId: Property<String>
}
