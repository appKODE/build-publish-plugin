@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.firebase

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.util.APK_FILE_EXTENSION
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getCommon
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseDistributionConfig
import ru.kode.android.build.publish.plugin.firebase.extension.BuildPublishFirebaseExtension
import java.io.File

private const val EXTENSION_NAME = "buildPublishFirebase"

/**
 * A Gradle plugin that configures Firebase App Distribution for Android projects.
 *
 * This plugin:
 * - Integrates with the Firebase App Distribution Gradle Plugin
 * - Configures distribution settings for different build variants
 * - Automatically includes changelogs in distribution releases
 * - Supports configuration of test groups, service credentials, and artifact types
 *
 * The plugin is designed to work with the build publishing system and requires
 * the Firebase App Distribution plugin to be applied to the project.
 */
abstract class BuildPublishFirebasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        val changelogFile = project.changelogDirectory()

        val buildPublishFirebaseExtension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishFirebaseExtension::class.java)

        androidExtension.finalizeDsl {
            val firebaseAppDistributionConfig =
                buildPublishFirebaseExtension
                    .distribution
                    // NOTE: NamedDomainObjectContainer can be resolved only in task on after finalizeDsl,
                    // because it can be defined after plugin application
                    .getCommon()

            if (firebaseAppDistributionConfig != null) {
                project.pluginManager.apply(AppDistributionPlugin::class.java)
            }
            project.configurePlugin(
                firebaseDistributionConfig = firebaseAppDistributionConfig,
                changelogFile = changelogFile.get().asFile,
            )
        }
    }
}

/**
 * Configures the Firebase App Distribution plugin with the provided settings.
 *
 * @param firebaseDistributionConfig The configuration for Firebase App Distribution
 * @param changelogFile The file containing release notes for the distribution
 */
private fun Project.configurePlugin(
    firebaseDistributionConfig: FirebaseDistributionConfig?,
    changelogFile: File,
) {
    plugins.all { plugin ->
        when (plugin) {
            is AppDistributionPlugin -> {
                if (firebaseDistributionConfig != null) {
                    val appDistributionExtension =
                        extensions
                            .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        config = firebaseDistributionConfig,
                        changelogFile = changelogFile,
                    )
                }
            }
        }
    }
}

/**
 * Configures the AppDistributionExtension with the provided settings.
 *
 * @param config The Firebase distribution configuration
 * @param changelogFile The file containing release notes
 */
private fun AppDistributionExtension.configure(
    config: FirebaseDistributionConfig,
    changelogFile: File,
) {
    val serviceCredentialsFilePath =
        config
            .serviceCredentialsFile
            .orNull
            ?.asFile
            ?.path
            ?.takeIf { it.isNotBlank() }
    val applicationId =
        config
            .appId
            .orNull
            ?.takeIf { it.isNotBlank() }
    val testerGroups = config.testerGroups.get()
    val artifactType = config.artifactType.orNull ?: APK_FILE_EXTENSION.uppercase()

    if (applicationId != null) {
        appId = applicationId
    }
    serviceCredentialsFile = serviceCredentialsFilePath.orEmpty()
    releaseNotesFile = changelogFile.path
    this.artifactType = artifactType
    this.groups = testerGroups.joinToString(",")
}
