package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration for automating ClickUp tasks during the build process.
 *
 * This interface defines the automation settings for interacting with ClickUp tasks.
 * It's typically configured in the build script using the `automation` block of the `buildPublishClickUp` extension.
 */
interface ClickUpAutomationConfig {
    val name: String

    /**
     * Pattern used to format version numbers for ClickUp tasks.
     *
     * This pattern is used to format the version number when setting fix versions in ClickUp.
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The ID of the custom field used for fix versions in ClickUp.
     *
     * This is the ID of the custom field in ClickUp where the fix version will be set.
     * You can find this ID by inspecting the network requests in your browser's developer tools
     * when viewing a task in ClickUp.
     */
    @get:Input
    @get:Optional
    val fixVersionFieldId: Property<String>

    /**
     * The tag name to apply to ClickUp tasks during this build.
     *
     * This tag will be added to all tasks that are processed during the build.
     * It's typically set to the version name or build number to track which
     * release a task was included in.
     */
    @get:Input
    @get:Optional
    val tagName: Property<String>
}
