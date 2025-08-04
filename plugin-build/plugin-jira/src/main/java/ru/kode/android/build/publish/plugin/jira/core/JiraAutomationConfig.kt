package ru.kode.android.build.publish.plugin.jira.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface JiraAutomationConfig {
    val name: String

    @get:Input
    val projectId: Property<Long>

    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    @get:Input
    @get:Optional
    val resolvedStatusTransitionId: Property<String>

}
