package ru.kode.android.build.publish.plugin.clickup.core

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile

interface ClickUpAuthConfig {
    val name: String

    /**
     * The path to the file containing the API token for the ClickUp
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty
}
