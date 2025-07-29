@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.telegram

import org.gradle.api.Plugin
import org.gradle.api.Project

interface BuildPublishPluginCore : Plugin<Project> {
    override fun apply(project: Project) {

    }
}
