package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface ChangelogSettingsConfig {
    val name: String

    /**
     * How task number formatted
     * For example:  "BASE-\\d+"
     */
    @get:Input
    val issueNumberPattern: Property<String>

    /**
     * Address of task tracker
     * For example: "https://jira.example.ru/browse/"
     */
    @get:Input
    val issueUrlPrefix: Property<String>

    /**
     * Application bundle name for changelog
     * For example: example-base-project-android
     */
    @get:Input
    val baseOutputFileName: Property<String>

    /**
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    @get:Input
    val commitMessageKey: Property<String>
}
