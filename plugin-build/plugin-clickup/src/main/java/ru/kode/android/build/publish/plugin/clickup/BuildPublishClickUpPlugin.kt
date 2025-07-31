@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.clickup

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.clickup.extensions.BuildPublishClickUpExtension

private const val CLICK_UP_EXTENSION_NAME = "buildPublishClickUp"

abstract class BuildPublishClickUpPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(CLICK_UP_EXTENSION_NAME, BuildPublishClickUpExtension::class.java)
    }
}
