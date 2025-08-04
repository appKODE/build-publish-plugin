@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.confluence

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.confluence.extensions.BuildPublishConfluenceExtension

private const val EXTENSION_NAME = "buildPublishConfluence"

abstract class BuildPublishConfluencePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(EXTENSION_NAME, BuildPublishConfluenceExtension::class.java)
    }
}
