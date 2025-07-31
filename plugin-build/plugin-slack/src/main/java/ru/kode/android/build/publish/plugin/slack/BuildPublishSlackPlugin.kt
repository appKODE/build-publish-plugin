@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.slack

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.slack.extensions.BuildPublishSlackExtension

private const val SLACK_EXTENSION_NAME = "buildPublishSlack"

abstract class BuildPublishSlackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(SLACK_EXTENSION_NAME, BuildPublishSlackExtension::class.java)
    }
}
