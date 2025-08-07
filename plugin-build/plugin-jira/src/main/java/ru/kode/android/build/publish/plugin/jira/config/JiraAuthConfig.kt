package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface JiraAuthConfig {
    val name: String

    @get:Input
    val baseUrl: Property<String>

    @get:Input
    val authUsername: Property<String>

    @get:Input
    val authPassword: Property<String>
}
