package ru.kode.android.firebase.publish.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.util.internal.VersionNumber
import ru.kode.android.firebase.publish.plugin.configuration.configure
import ru.kode.android.firebase.publish.plugin.task.SendChangelogTask

internal const val SEND_CHANGELOG_TASK_PREFIX = "sendChangelog"
internal const val BUILD_PUBLISH_TASK_PREFIX = "processBuildPublish"
internal const val DISTRIBUTION_UPLOAD_TASK_PREFIX = "appDistributionUpload"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class FirebasePublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val firebasePublishExtension = project.extensions
            .create(EXTENSION_NAME, FirebasePublishExtension::class.java, project)
        if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
            throw StopExecutionException(
                "Must only be used with with Android Gradle Plugin >= 7.4 "
            )
        }
        if (!project.plugins.hasPlugin(AppPlugin::class.java)) {
            throw StopExecutionException(
                "Must only be used with Android application projects." +
                    " Please apply the 'com.android.application' plugin."
            )
        }

        if (!project.plugins.hasPlugin(AppDistributionPlugin::class.java)) {
            throw StopExecutionException(
                "Must only be used with Firebase App Distribution." +
                    " Please apply the 'com.google.firebase.appdistribution' plugin."
            )
        }

        val androidExtensions =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtensions.finalizeDsl { ext ->
            val buildVariants = prepareBuildVariants(ext)
            if (buildVariants.isEmpty()) {
                throw StopExecutionException(
                    "Build types or(and) flavors not configured for android project. " +
                        "Please add something of this"
                )
            }
            project.plugins.all { plugin ->
                when (plugin) {
                    is AppDistributionPlugin -> {
                        configureAppDistribution(firebasePublishExtension, project, buildVariants)
                    }
                }
            }

            buildVariants.forEach { buildVariant ->
                val capitalizedBuildVariant = buildVariant.capitalize()
                project.tasks.apply {
                    registerSendChangelogTask(
                        capitalizedBuildVariant,
                        buildVariants,
                        firebasePublishExtension
                    )
                    registerAppDistributionPublishTask(capitalizedBuildVariant)
                }
            }
            project.logger.debug("result tasks ${project.tasks.map { it.name }}")
        }
    }

    private fun TaskContainer.registerAppDistributionPublishTask(
        capitalizedBuildVariant: String
    ) {
        register("$BUILD_PUBLISH_TASK_PREFIX$capitalizedBuildVariant") {
            it.dependsOn("$DISTRIBUTION_UPLOAD_TASK_PREFIX$capitalizedBuildVariant")
            it.finalizedBy("$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant")
        }
    }

    private fun TaskContainer.registerSendChangelogTask(
        capitalizedBuildVariant: String,
        buildVariants: Set<String>,
        firebasePublishExtension: FirebasePublishExtension
    ) {
        register(
            "$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            SendChangelogTask::class.java
        ) {
            it.buildVariants.set(buildVariants)
            it.baseOutputFileName.set(firebasePublishExtension.baseOutputFileName)
            it.commitMessageKey.set(firebasePublishExtension.commitMessageKey)
            it.tgUserMentions.set(firebasePublishExtension.tgUserMentions)
            it.slackUserMentions.set(firebasePublishExtension.slackUserMentions)
            it.slackConfig.set(firebasePublishExtension.slackConfig)
            it.issueUrlPrefix.set(firebasePublishExtension.issueUrlPrefix)
            it.issueNumberPattern.set(firebasePublishExtension.issueNumberPattern)
            it.tgConfig.set(firebasePublishExtension.tgConfig)
        }
    }

    private fun configureAppDistribution(
        firebasePublishExtension: FirebasePublishExtension,
        project: Project,
        buildVariants: Set<String>
    ) {
        // TODO Can't get actual values, add different logic
        val distributionServiceKey =
            firebasePublishExtension.distributionServiceKey.get()
        val commitMessageKey = firebasePublishExtension.commitMessageKey.get()
        val testerGroups = firebasePublishExtension.distributionTesterGroups.get()
        project.logger.debug("testerGroups = $testerGroups")
        project.extensions.getByType(AppDistributionExtension::class.java)
            .configure(
                project,
                distributionServiceKey,
                commitMessageKey,
                buildVariants,
                testerGroups
            )
    }

    private fun prepareBuildVariants(ext: ApplicationExtension): Set<String> {
        return if (ext.productFlavors.isEmpty()) {
            ext.buildTypes.map { it.name }.toSet()
        } else {
            ext.productFlavors.flatMap { flavor ->
                ext.buildTypes.map { "${flavor.name}${it.name.capitalize()}" }
            }.toSet()
        }
    }
}
