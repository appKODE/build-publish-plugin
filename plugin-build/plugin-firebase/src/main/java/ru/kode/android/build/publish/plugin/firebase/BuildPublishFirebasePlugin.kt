@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.firebase

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getDefault
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseDistributionConfig
import ru.kode.android.build.publish.plugin.firebase.extensions.BuildPublishFirebaseExtension
import java.io.File

private const val EXTENSION_NAME = "buildPublishFirebase"

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
                    .getDefault()

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

private fun AppDistributionExtension.configure(
    config: FirebaseDistributionConfig,
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
