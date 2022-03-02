package ru.kode.android.firebase.publish.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.util.internal.VersionNumber
import ru.kode.android.firebase.publish.plugin.command.LinuxShellCommandExecutor
import ru.kode.android.firebase.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.firebase.publish.plugin.git.GitRepository
import ru.kode.android.firebase.publish.plugin.task.SendChangelogTask
import ru.kode.android.firebase.publish.plugin.util.Changelog

internal const val SEND_CHANGELOG_TASK_PREFIX = "sendChangelog"
internal const val BUILD_PUBLISH_TASK_PREFIX = "processBuildPublish"
internal const val DISTRIBUTION_UPLOAD_TASK_PREFIX = "appDistributionUpload"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class FirebasePublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        val firebasePublishExtension = project.extensions
            .create(EXTENSION_NAME, FirebasePublishExtension::class.java, project)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl { ext ->
            val buildVariants = prepareBuildVariants(ext)
            if (buildVariants.isEmpty()) {
                throw StopExecutionException(
                    "Build types or(and) flavors not configured for android project. " +
                        "Please add something of this"
                )
            }
            project.configurePlugins(firebasePublishExtension, buildVariants)
            project.registerTasks(firebasePublishExtension, buildVariants)
            project.logger.debug("result tasks ${project.tasks.map { it.name }}")
        }
    }

    private fun Project.registerTasks(
        firebasePublishExtension: FirebasePublishExtension,
        buildVariants: Set<String>,
    ) {
        buildVariants.forEach { buildVariant ->
            val capitalizedBuildVariant = buildVariant.capitalize()
            tasks.apply {
                registerSendChangelogTask(
                    capitalizedBuildVariant,
                    buildVariants,
                    firebasePublishExtension
                )
                registerAppDistributionPublishTask(capitalizedBuildVariant)
            }
        }
    }

    private fun Project.configurePlugins(
        firebasePublishExtension: FirebasePublishExtension,
        buildVariants: Set<String>,
    ) {
        plugins.all { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    val appExtension = extensions.getByType(AppExtension::class.java)
                    appExtension.configure(firebasePublishExtension, this, buildVariants)
                }
                is AppDistributionPlugin -> {
                    val appDistributionExtension = extensions
                        .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        firebasePublishExtension = firebasePublishExtension,
                        project = this,
                        buildVariants = buildVariants
                    )
                }
            }
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

@Suppress("ThrowsCount") // block to throws exceptions on apply
private fun Project.stopExecutionIfNotSupported() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
        throw StopExecutionException(
            "Must only be used with with Android Gradle Plugin >= 7.4 "
        )
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Android application projects." +
                " Please apply the 'com.android.application' plugin."
        )
    }

    if (!plugins.hasPlugin(AppDistributionPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Firebase App Distribution." +
                " Please apply the 'com.google.firebase.appdistribution' plugin."
        )
    }
}

private fun AppExtension.configure(
    firebasePublishExtension: FirebasePublishExtension,
    project: Project,
    buildVariants: Set<String>
) {
    defaultConfig {
        val commandExecutor = LinuxShellCommandExecutor(project)
        val repository = GitRepository(commandExecutor, buildVariants)
        val mostRecentTag = repository.findMostRecentBuildTag()
        val initialBuildNumber = firebasePublishExtension.initialBuildNumber.orNull ?: 0
        val versionCode = initialBuildNumber + (mostRecentTag?.buildNumber ?: 1)
        project.logger.debug("versionCode = $versionCode")

        it.versionCode = versionCode
        it.versionName = mostRecentTag?.name ?: "v0.0-dev"
    }
}

private fun AppDistributionExtension.configure(
    firebasePublishExtension: FirebasePublishExtension,
    project: Project,
    buildVariants: Set<String>
) {
    val distributionServiceKey =
        firebasePublishExtension.distributionServiceKey.get()
    val commitMessageKey = firebasePublishExtension.commitMessageKey.get()
    val testerGroups = firebasePublishExtension.distributionTesterGroups.get()
    project.logger.debug("testerGroups = $testerGroups")

    val commandExecutor = LinuxShellCommandExecutor(project)
    serviceCredentialsFile = System.getenv(distributionServiceKey).orEmpty()
    releaseNotes = buildChangelog(project, commandExecutor, commitMessageKey, buildVariants)
    this.groups = testerGroups.joinToString(",")
}

private fun buildChangelog(
    project: Project,
    commandExecutor: ShellCommandExecutor,
    messageKey: String,
    buildVariants: Set<String>,
): String {
    return Changelog(commandExecutor, project.logger, messageKey, buildVariants)
        .buildForRecentBuildTag()
        .also {
            if (it.isNullOrBlank()) {
                project.logger.warn("App Distribution changelog is empty")
            }
        }
        .orEmpty()
}
