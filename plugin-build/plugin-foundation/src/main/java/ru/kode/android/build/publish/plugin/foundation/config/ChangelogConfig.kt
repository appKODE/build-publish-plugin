package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface ChangelogConfig {
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
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    @get:Input
    val commitMessageKey: Property<String>
}
