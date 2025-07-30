package ru.kode.android.build.publish.plugin.clickup.core

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpAutomationTask


interface ClickUpConfig {
    val name: String

    /**
     * The path to the file containing the API token for the ClickUp
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * Pattern to be used to format version to the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The id of the custom field to be used for the fix version in the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val fixVersionFieldId: Property<String>

    /**
     * The tag name to be used for the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val tagName: Property<String>
}
