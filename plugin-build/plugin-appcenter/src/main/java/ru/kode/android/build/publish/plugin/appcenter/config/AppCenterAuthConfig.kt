package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

interface AppCenterAuthConfig {
    val name: String

    /**
     * The path to JSON file with token for App Center project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * Owner name of the App Center project
     */
    @get:Input
    val ownerName: Property<String>
}
