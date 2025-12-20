@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.firebase

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.firebase.extension.BuildPublishFirebaseExtension

internal const val EXTENSION_NAME = "buildPublishFirebase"

/**
 * Gradle plugin that configures Firebase App Distribution
 * using variant-aware Android Components API.
 */
abstract class BuildPublishFirebasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

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
