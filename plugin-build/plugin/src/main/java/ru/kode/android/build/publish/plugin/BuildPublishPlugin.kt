@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.enity.BuildVariant
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.task.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.SendChangelogTask
import ru.kode.android.build.publish.plugin.util.capitalized
import ru.kode.android.build.publish.plugin.util.concatenated
import java.io.File

internal const val SEND_CHANGELOG_TASK_PREFIX = "sendChangelog"
internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"
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
        androidExtension.onVariants(
            callback = { variant ->
                val commandExecutor = getCommandExecutor(project)
                val repository = GitRepository(commandExecutor, setOf(variant.name))
                val recentBuildTag = repository.findRecentBuildTag()
                val versionCode = recentBuildTag?.buildNumber ?: 1
                val versionName = recentBuildTag?.name ?: "v0.0-dev"

                variant.outputs.forEach { output ->
                    if (output is ApkVariantOutputImpl) {
                        output.versionCodeOverride = versionCode
                        output.versionNameOverride = versionName
                    }
                }
            }
        )
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
            val changelogFile = File(project.buildDir, "release-notes.txt")
            project.configurePlugins(firebasePublishExtension, changelogFile)
            project.registerTasks(firebasePublishExtension, buildVariantNames, changelogFile)
            project.logger.debug("result tasks ${project.tasks.map { it.name }}")
        }
    }

    private fun Project.registerTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariants: Set<String>,
        changelogFile: File,
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
            tasks.apply {
                registerGenerateChangelogTask(
                    buildPublishExtension,
                    buildVariant,
                    changelogFile
                )
                registerSendChangelogTask(
                    buildPublishExtension,
                    buildVariant,
                    changelogFile
                )
                registerAppDistributionPublishTask(buildVariant)
            }
        }
    }

    private fun Project.configurePlugins(
        buildPublishExtension: BuildPublishExtension,
        changelogFile: File,
    ) {
        plugins.all { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    val appExtension = extensions.getByType(AppExtension::class.java)
                    appExtension.configure()
                }
                is AppDistributionPlugin -> {
                    val appDistributionExtension = extensions
                        .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        buildPublishExtension = buildPublishExtension,
                        changelogFile = changelogFile,
                    )
                }
            }
        }
    }

    private fun TaskContainer.registerAppDistributionPublishTask(
        buildVariant: String
    ) {
        val capitalizedBuildVariant = buildVariant.capitalized()
        register("$BUILD_PUBLISH_TASK_PREFIX$capitalizedBuildVariant") {
            it.dependsOn("$DISTRIBUTION_UPLOAD_TASK_PREFIX$capitalizedBuildVariant")
            it.finalizedBy("$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant")
        }
    }

    private fun TaskContainer.registerGenerateChangelogTask(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: String,
        changelogFile: File,
    ) {
        val capitalizedBuildVariant = buildVariant.capitalized()
        register(
            "$GENERATE_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            GenerateChangelogTask::class.java
        ) {
            it.commitMessageKey.set(buildPublishExtension.commitMessageKey)
            it.buildVariant.set(buildVariant)
            it.changelogFile.set(changelogFile)
        }
    }

    private fun TaskContainer.registerSendChangelogTask(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: String,
        changelogFile: File,
    ) {
        val capitalizedBuildVariant = buildVariant.capitalized()
        register(
            "$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            SendChangelogTask::class.java
        ) {
            it.buildVariant.set(buildVariant)
            it.changelogFile.set(changelogFile)
            it.issueUrlPrefix.set(buildPublishExtension.issueUrlPrefix)
            it.issueNumberPattern.set(buildPublishExtension.issueNumberPattern)
            it.baseOutputFileName.set(buildPublishExtension.baseOutputFileName)
            it.slackUserMentions.set(buildPublishExtension.slackUserMentions)
            it.slackConfig.set(buildPublishExtension.slackConfig)
            it.tgUserMentions.set(buildPublishExtension.tgUserMentions)
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

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = 1
        it.versionName = "v0.0-dev"
    }
}

private fun AppDistributionExtension.configure(
    buildPublishExtension: BuildPublishExtension,
    changelogFile: File,
) {
    val serviceCredentialsFilePath = buildPublishExtension
        .distributionServiceCredentialsFilePath.orNull
        ?.takeIf { it.isNotBlank() }
    val applicationId = buildPublishExtension
        .distributionAppId.orNull
        ?.takeIf { it.isNotBlank() }
    val testerGroups = buildPublishExtension.distributionTesterGroups.get()
    val artifactType = buildPublishExtension.distributionArtifactType.get()

    if (applicationId != null) {
        appId = applicationId
    }
    serviceCredentialsFile = serviceCredentialsFilePath.orEmpty()
    releaseNotesFile = changelogFile.path
    this.artifactType = artifactType
    this.groups = testerGroups.joinToString(",")
}
