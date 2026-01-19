@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.firebase

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.firebase.extension.BuildPublishFirebaseExtension
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.firebase.messages.mustApplyFoundationPluginMessage

internal const val EXTENSION_NAME = "buildPublishFirebase"

/**
 * Gradle plugin that configures Firebase App Distribution using the Android Components API.
 *
 * The plugin creates the [BuildPublishFirebaseExtension] and, if at least one distribution config
 * is declared, applies the official Firebase App Distribution Gradle plugin ([AppDistributionPlugin])
 * so that [com.google.firebase.appdistribution.gradle.AppDistributionVariantExtension] is available.
 */
abstract class BuildPublishFirebasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(mustApplyFoundationPluginMessage())
        }

        val buildPublishFirebaseExtension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishFirebaseExtension::class.java,
            )

        androidComponents.finalizeDsl {
            val hasDistributionConfig =
                buildPublishFirebaseExtension
                    .distribution
                    .isNotEmpty()

            if (hasDistributionConfig) {
                project.pluginManager.apply(AppDistributionPlugin::class.java)
            }
        }
    }
}
