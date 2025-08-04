@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.appcenter

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.appcenter.extensions.BuildPublishAppCenterExtension

private const val EXTENSION_NAME = "buildPublishAppCenter"

abstract class BuildPublishAppCenterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(EXTENSION_NAME, BuildPublishAppCenterExtension::class.java)
    }
}
