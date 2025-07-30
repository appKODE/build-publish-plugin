@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.firebase

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseAppDistributionConfig
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.file.RegularFileProperty
import ru.kode.android.build.publish.plugin.firebase.extensions.BuildPublishExtension
import java.io.File

internal const val DEFAULT_CONTAINER_NAME = "default"
const val EXTENSION_NAME = "buildPublish"

interface BuildPublishFirebasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        val changelogFile: RegularFileProperty? = null

        // TODO: Create another extensions
        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java)

        androidExtension.finalizeDsl {
            val firebaseAppDistributionConfig =
                buildPublishExtension
                    .firebaseDistribution
                    // NOTE: NamedDomainObjectContainer can be resolved only in task on after finalizeDsl,
                    // because it can be defined after plugin application
                    .findByName(DEFAULT_CONTAINER_NAME)

            if (firebaseAppDistributionConfig != null) {
                project.pluginManager.apply(AppDistributionPlugin::class.java)
            }
            project.configurePlugin(
                firebaseAppDistributionConfig = firebaseAppDistributionConfig,
                changelogFile = changelogFile!!.get().asFile,
            )
        }
    }
}

private fun Project.configurePlugin(
    firebaseAppDistributionConfig: FirebaseAppDistributionConfig?,
    changelogFile: File,
) {
    plugins.all { plugin ->
        when (plugin) {
            is AppDistributionPlugin -> {
                if (firebaseAppDistributionConfig != null) {
                    val appDistributionExtension =
                        extensions
                            .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        config = firebaseAppDistributionConfig,
                        changelogFile = changelogFile,
                    )
                }
            }
        }
    }
}

private fun AppDistributionExtension.configure(
    config: FirebaseAppDistributionConfig,
    changelogFile: File,
) {
    val serviceCredentialsFilePath =
        config
            .serviceCredentialsFilePath
            .orNull
            ?.takeIf { it.isNotBlank() }
    val applicationId =
        config
            .appId
            .orNull
            ?.takeIf { it.isNotBlank() }
    val testerGroups = config.testerGroups.get()
    val artifactType = config.artifactType.orNull ?: "APK"

    if (applicationId != null) {
        appId = applicationId
    }
    serviceCredentialsFile = serviceCredentialsFilePath.orEmpty()
    releaseNotesFile = changelogFile.path
    this.artifactType = artifactType
    this.groups = testerGroups.joinToString(",")
}
