@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.telegram

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.telegram.extensions.BuildPublishTelegramExtension

private const val TELEGRAM_EXTENSION_NAME = "buildPublishTelegram"

abstract class BuildPublishTelegramPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(TELEGRAM_EXTENSION_NAME, BuildPublishTelegramExtension::class.java)
    }
}
