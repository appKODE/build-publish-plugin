@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

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
import ru.kode.android.build.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.enity.BuildVariant
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.task.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.SendChangelogTask
import ru.kode.android.build.publish.plugin.util.Changelog
import ru.kode.android.build.publish.plugin.util.capitalized
import ru.kode.android.build.publish.plugin.util.concatenated

internal const val SEND_CHANGELOG_TASK_PREFIX = "sendChangelog"
internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val BUILD_PUBLISH_TASK_PREFIX = "processBuildPublish"
internal const val DISTRIBUTION_UPLOAD_TASK_PREFIX = "appDistributionUpload"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        val firebasePublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java, project)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl { ext ->
            val buildVariantNames: Set<String> = ext
                .extractBuildVariants()
                .mapTo(mutableSetOf()) { it.concatenated() }
            if (buildVariantNames.isEmpty()) {
                throw StopExecutionException(
                    "Build types or(and) flavors not configured for android project. " +
                        "Please add something of this"
                )
            }
            project.configurePlugins(firebasePublishExtension, buildVariantNames)
            project.registerTasks(firebasePublishExtension, buildVariantNames)
            project.logger.debug("result tasks ${project.tasks.map { it.name }}")
        }
    }

    private fun Project.registerTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariants: Set<String>,
    ) {
        tasks.register(
            PRINT_LAST_INCREASED_TAG_TASK_PREFIX,
            PrintLastIncreasedTag::class.java
        ) { task ->
            task.buildVariants.set(buildVariants)
            val variantProperty = project.findProperty("variant") as? String
            task.variant.set(variantProperty.takeIf { it?.isNotBlank() == true }?.trim())
        }
        buildVariants.forEach { buildVariant ->
            val capitalizedBuildVariant = buildVariant.capitalized()
            tasks.apply {
                registerSendChangelogTask(
                    capitalizedBuildVariant,
                    buildVariants,
                    buildPublishExtension
                )
                registerAppDistributionPublishTask(capitalizedBuildVariant)
            }
        }
    }

    private fun Project.configurePlugins(
        buildPublishExtension: BuildPublishExtension,
        buildVariants: Set<String>,
    ) {
        plugins.all { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    val appExtension = extensions.getByType(AppExtension::class.java)
                    appExtension.configure(this, buildVariants)
                }
                is AppDistributionPlugin -> {
                    val appDistributionExtension = extensions
                        .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        buildPublishExtension = buildPublishExtension,
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
        buildPublishExtension: BuildPublishExtension
    ) {
        register(
            "$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            SendChangelogTask::class.java
        ) {
            it.buildVariants.set(buildVariants)
            it.baseOutputFileName.set(buildPublishExtension.baseOutputFileName)
            it.commitMessageKey.set(buildPublishExtension.commitMessageKey)
            it.tgUserMentions.set(buildPublishExtension.tgUserMentions)
            it.slackUserMentions.set(buildPublishExtension.slackUserMentions)
            it.slackConfig.set(buildPublishExtension.slackConfig)
            it.issueUrlPrefix.set(buildPublishExtension.issueUrlPrefix)
            it.issueNumberPattern.set(buildPublishExtension.issueNumberPattern)
            it.tgConfig.set(buildPublishExtension.tgConfig)
        }
    }
}

private fun ApplicationExtension.extractBuildVariantFlavors(): Set<List<String>> {
    if (flavorDimensions.size <= 1) return productFlavors.mapTo(mutableSetOf()) { listOf(it.name) }

    val dimensionToFlavorName = productFlavors.groupBy(
        keySelector = { it.dimension },
        valueTransform = { it.name }
    )
    return flavorDimensions
        .mapNotNull { dimensionToFlavorName[it] }
        .fold(setOf(listOf())) { flavorCombinations, flavors ->
            if (flavorCombinations.isEmpty()) {
                return@fold flavorCombinations + setOf(flavors)
            }
            flavorCombinations.flatMapTo(mutableSetOf()) { flavorCombination ->
                flavors.map { flavor ->
                    flavorCombination + flavor
                }
            }
        }
}

private fun ApplicationExtension.extractBuildVariants(): Set<BuildVariant> {
    return extractBuildVariantFlavors()
        .ifEmpty { setOf(emptyList()) }
        .flatMapTo(mutableSetOf()) { flavors ->
            buildTypes
                .map { buildType ->
                    BuildVariant(
                        flavorNames = flavors,
                        buildTypeName = buildType.name
                    )
                }
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
    project: Project,
    buildVariants: Set<String>
) {
    defaultConfig {
        val commandExecutor = getCommandExecutor(project)
        val repository = GitRepository(commandExecutor, buildVariants)
        val mostRecentTag = repository.findMostRecentBuildTag()
        val versionCode = mostRecentTag?.buildNumber ?: 1
        project.logger.debug("versionCode = $versionCode")

        it.versionCode = versionCode
        it.versionName = mostRecentTag?.name ?: "v0.0-dev"
    }
}

private fun AppDistributionExtension.configure(
    buildPublishExtension: BuildPublishExtension,
    project: Project,
    buildVariants: Set<String>,
) {
    val serviceCredentialsFilePath = buildPublishExtension
        .distributionServiceCredentialsFilePath.orNull
        ?.takeIf { it.isNotBlank() }
    val applicationId = buildPublishExtension
        .distributionAppId.orNull
        ?.takeIf { it.isNotBlank() }
    val commitMessageKey = buildPublishExtension.commitMessageKey.get()
    val testerGroups = buildPublishExtension.distributionTesterGroups.get()
    val artifactType = buildPublishExtension.distributionArtifactType.get()
    project.logger.debug("testerGroups = $testerGroups")
    project.logger.debug("artifactType = $artifactType")

    val commandExecutor = getCommandExecutor(project)
    if (applicationId != null) {
        appId = applicationId
    }
    serviceCredentialsFile = serviceCredentialsFilePath.orEmpty()
    releaseNotes = buildChangelog(project, commandExecutor, commitMessageKey, buildVariants)
    this.artifactType = artifactType
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
