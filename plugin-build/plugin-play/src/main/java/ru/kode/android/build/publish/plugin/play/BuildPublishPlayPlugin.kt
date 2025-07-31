@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.play

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.play.extensions.BuildPublishPlayExtension

private const val PLAY_EXTENSION_NAME = "buildPublishPlay"

abstract class BuildPublishPlayPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(PLAY_EXTENSION_NAME, BuildPublishPlayExtension::class.java)
    }
}
