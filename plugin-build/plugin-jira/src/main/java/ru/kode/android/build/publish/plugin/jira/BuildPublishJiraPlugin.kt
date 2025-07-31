@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.jira.extensions.BuildPublishJiraExtension

private const val JIRA_EXTENSION_NAME = "buildPublishJira"

abstract class BuildPublishJiraPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(JIRA_EXTENSION_NAME, BuildPublishJiraExtension::class.java)
    }
}
