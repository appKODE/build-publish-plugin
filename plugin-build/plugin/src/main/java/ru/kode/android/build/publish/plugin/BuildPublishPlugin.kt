@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.enity.BuildVariant
import ru.kode.android.build.publish.plugin.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.GetLastTagTask
import ru.kode.android.build.publish.plugin.task.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.SendChangelogTask
import ru.kode.android.build.publish.plugin.task.appcenter.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.util.capitalizedName
import java.io.File

internal const val SEND_CHANGELOG_TASK_PREFIX = "sendChangelog"
internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"
internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"
internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java, project)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFile = File(project.buildDir, "changelog.txt")

        androidExtension.onVariants { variant ->
            val output = variant.outputs
                .find { it is VariantOutputImpl && it.fullName == variant.name }
                as? VariantOutputImpl
            if (output != null) {
                val outputFile = output.outputFileName.flatMap { fileName ->
                    project.tasks.withType(PackageAndroidArtifact::class.java)
                        .firstOrNull { it.variantName == variant.name }
                        ?.outputDirectory
                        ?.flatMap { directory ->
                            project.objects
                                .fileProperty()
                                .apply { set(File(directory.asFile, fileName)) }
                        }
                        ?: throw GradleException("no output for variant ${variant.name}")
                }
                val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                project.registerVariantTasks(buildPublishExtension, buildVariant, changelogFile, outputFile)
                val getLastTagTask = project.tasks
                    .findByName("${GET_LAST_TAG_TASK_PREFIX}${buildVariant.capitalizedName()}")
                    as GetLastTagTask
                output.versionCode.set(
                    getLastTagTask.tagBuildFile.map {
                        val file = it.asFile
                        if (file.exists()) fromJson(file).buildNumber else 1
                    }
                )
                output.versionName.set(
                    getLastTagTask.tagBuildFile.map {
                        val file = it.asFile
                        if (file.exists()) fromJson(file).name else "v0.0-dev"
                    }
                )
            }
        }

        androidExtension.finalizeDsl { ext ->
            project.configurePlugins(buildPublishExtension, changelogFile)
        }
    }

    private fun Project.registerVariantTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFile: File,
        outputFile: Provider<RegularFile>,
    ) {
        tasks.apply {
            val getLastTagTaskProvider = registerGetLastTagTask(buildVariant)
            registerPrintLastIncreasedTagTask(
                buildVariant,
                getLastTagTaskProvider.flatMap { it.tagBuildFile }
            )
            val generateChangelogTaskProvider = registerGenerateChangelogTask(
                buildPublishExtension,
                buildVariant,
                changelogFile,
                getLastTagTaskProvider.flatMap { it.tagBuildFile }
            )
            registerAppCenterDistributionTask(
                buildPublishExtension,
                buildVariant,
                generateChangelogTaskProvider.flatMap { it.changelogFile },
                outputFile,
            )
            registerSendChangelogTask(
                buildPublishExtension,
                buildVariant,
                generateChangelogTaskProvider.flatMap { it.changelogFile },
                getLastTagTaskProvider.flatMap { it.tagBuildFile }
            )
        }
    }

    private fun Project.registerGetLastTagTask(
        buildVariant: BuildVariant,
    ): Provider<GetLastTagTask> {
        val capitalizedBuildVariant = buildVariant.capitalizedName()
        return tasks.register(
            "$GET_LAST_TAG_TASK_PREFIX$capitalizedBuildVariant",
            GetLastTagTask::class.java
        ) { task ->
            val tagBuildFile = project.layout.buildDirectory
                .file("tag-build-${buildVariant.name}.json")
            task.tagBuildFile.set(tagBuildFile)
            task.buildVariant.set(buildVariant.name)
        }
    }

    private fun TaskContainer.registerPrintLastIncreasedTagTask(
        buildVariant: BuildVariant,
        tagBuildFileProvider: Provider<RegularFile>
    ) {
        val capitalizedBuildVariant = buildVariant.capitalizedName()
        register(
            "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX$capitalizedBuildVariant",
            PrintLastIncreasedTag::class.java
        ) { task ->
            task.tagBuildFile.set(tagBuildFileProvider)
        }
    }

    private fun TaskContainer.registerGenerateChangelogTask(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFile: File,
        tagBuildFileProvider: Provider<RegularFile>
    ): Provider<GenerateChangelogTask> {
        val capitalizedBuildVariant = buildVariant.capitalizedName()
        return register(
            "$GENERATE_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            GenerateChangelogTask::class.java
        ) {
            it.commitMessageKey.set(buildPublishExtension.commitMessageKey)
            it.buildVariant.set(buildVariant.name)
            it.changelogFile.set(changelogFile)
            it.tagBuildFile.set(tagBuildFileProvider)
        }
    }

    private fun TaskContainer.registerAppCenterDistributionTask(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        outputFileProvider: Provider<RegularFile>,
    ): TaskProvider<AppCenterDistributionTask> {
        return register(
            "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            AppCenterDistributionTask::class.java,
        ) {
            it.config.set(buildPublishExtension.appCenterConfig)
            it.buildVariant.set(buildVariant)
            it.distributionGroups.set(buildPublishExtension.appCenterDistributionGroups)
            it.buildVariantOutputFile.set(outputFileProvider)
            it.changelogFile.set(changelogFileProvider)
        }
    }

    private fun TaskContainer.registerSendChangelogTask(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildFileProvider: Provider<RegularFile>,
    ) {
        val capitalizedBuildVariant = buildVariant.capitalizedName()
        register(
            "$SEND_CHANGELOG_TASK_PREFIX$capitalizedBuildVariant",
            SendChangelogTask::class.java
        ) {
            it.changelogFile.set(changelogFileProvider)
            it.tagBuildFile.set(tagBuildFileProvider)
            it.issueUrlPrefix.set(buildPublishExtension.issueUrlPrefix)
            it.issueNumberPattern.set(buildPublishExtension.issueNumberPattern)
            it.baseOutputFileName.set(buildPublishExtension.baseOutputFileName)
            it.slackUserMentions.set(buildPublishExtension.slackUserMentions)
            it.slackConfig.set(buildPublishExtension.slackConfig)
            it.tgUserMentions.set(buildPublishExtension.tgUserMentions)
            it.tgConfig.set(buildPublishExtension.tgConfig)
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
        it.versionName = "v0.0.1-dev"
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
