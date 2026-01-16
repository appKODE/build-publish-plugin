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
     * The name of the ClickUp workspace to work with.
     *
     * This is the name of the ClickUp workspace that the automation tasks will interact with.
     */
    @get:Input
    val workspaceName: Property<String>

    /**
     * Pattern used to format version numbers for ClickUp tasks.
     *
     * This pattern is formatted using `String.format(...)` and receives:
     * - `buildVersion`
     * - `buildNumber`
     * - `buildVariant`
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The name of the custom field used for fix versions in ClickUp.
     *
     * The task resolves this field name to a ClickUp custom field id using [workspaceName]
     * and updates the field value for each task.
     */
    @get:Input
    @get:Optional
    val fixVersionFieldName: Property<String>

    /**
     * The tag name to apply to ClickUp tasks during this build.
     *
     * This tag will be added to all tasks that are processed during the build.
     * It's typically set to the version name or build number to track which
     * release a task was included in.
     *
     * This pattern is formatted using `String.format(...)` and receives:
     * - `buildVersion`
     * - `buildNumber`
     * - `buildVariant`
     */
    @get:Input
    @get:Optional
    val tagPattern: Property<String>
}
