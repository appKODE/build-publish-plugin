@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import org.ajoberstar.grgit.gradle.GrgitService
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.appcenter.BuildPublishAppCenterPlugin
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.clickup.BuildPublishClickUpPlugin
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.confluence.BuildPublishConfluencePlugin
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableDefault
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredDefault
import ru.kode.android.build.publish.plugin.core.util.getDefault
import ru.kode.android.build.publish.plugin.extension.BuildPublishExtension
import ru.kode.android.build.publish.plugin.extension.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.firebase.BuildPublishFirebasePlugin
import ru.kode.android.build.publish.plugin.jira.BuildPublishJiraPlugin
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import ru.kode.android.build.publish.plugin.play.BuildPublishPlayPlugin
import ru.kode.android.build.publish.plugin.play.task.PlayTaskParams
import ru.kode.android.build.publish.plugin.play.task.PlayTasksRegistrar
import ru.kode.android.build.publish.plugin.slack.BuildPublishSlackPlugin
import ru.kode.android.build.publish.plugin.slack.task.SlackChangelogTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackDistributionTasksParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import ru.kode.android.build.publish.plugin.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.task.OutputProviders
import ru.kode.android.build.publish.plugin.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.telegram.BuildPublishTelegramPlugin
import ru.kode.android.build.publish.plugin.telegram.task.TelegramChangelogTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramDistributionTasksParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramTasksRegistrar

internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"

internal const val CHANGELOG_FILENAME = "changelog.txt"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFileProvider = project.layout.buildDirectory.file(CHANGELOG_FILENAME)
        val grgitService = project.extensions
            .getByType(GrgitServiceExtension::class.java)
            .service

        if (buildPublishExtension.jira.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishJiraPlugin::class.java)
        }

        if (buildPublishExtension.clickUp.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishClickUpPlugin::class.java)
        }

        if (buildPublishExtension.telegram.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishTelegramPlugin::class.java)
        }

        if (buildPublishExtension.confluence.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishConfluencePlugin::class.java)
        }

        if (buildPublishExtension.slack.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishSlackPlugin::class.java)
        }
//
        // TODO: Decide how to handle such extensions? Maybe it will be possible to do it dynamically
        val firebaseDistributionExtensions = buildPublishExtension
            .firebaseDistribution
            .getDefault()

        if (firebaseDistributionExtensions != null) {
            project.pluginManager.apply(BuildPublishFirebasePlugin::class.java)
        }

        if (buildPublishExtension.appCenterDistribution.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishAppCenterPlugin::class.java)
        }

        if (buildPublishExtension.play.isNotEmpty()) {
            project.pluginManager.apply(BuildPublishPlayPlugin::class.java)
        }

        androidExtension.onVariants(
            callback = { variant ->
                val variantOutput = variant.outputs
                    .find { it is VariantOutputImpl && it.fullName == variant.name }
                    as? VariantOutputImpl

                if (variantOutput != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val apkOutputFileName = variantOutput.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)

                    val outputConfig = buildPublishExtension.output.getByNameOrRequiredDefault(buildVariant.name)

                    val outputProviders = TagTasksRegistrar.registerLastTagTask(
                        project,
                        params = LastTagTaskParams(
                            buildVariant,
                            outputConfig,
                            apkOutputFileName,
                            grgitService,
                        )
                    )

                    TagTasksRegistrar.registerPrintLastIncreasedTagTask(
                        project = project,
                        params = PrintLastIncreasedTagTaskParams(
                            buildVariant,
                            outputProviders.tagBuildProvider,
                        )
                    )

                    val changelogConfig = buildPublishExtension.changelog.getByNameOrNullableDefault(buildVariant.name)

                    if (changelogConfig != null) {
                        val apkOutputFileProvider = outputProviders.apkOutputFileName.flatMap { fileName ->
                            project.mapToOutputApkFile(buildVariant, fileName)
                        }
                        project.registerChangelogDependentTasks(
                            changelogConfig,
                            buildPublishExtension,
                            buildVariant,
                            changelogFileProvider,
                            outputProviders,
                            grgitService,
                            apkOutputFileProvider,
                            bundleFile
                        )
                    }

                    if (outputProviders.versionCode != null) {
                        variantOutput.versionCode.set(outputProviders.versionCode)
                    }
                    if (outputProviders.versionName != null) {
                        variantOutput.versionName.set(outputProviders.versionName)
                    }
                    variantOutput.outputFileName.set(outputProviders.apkOutputFileName)
                }
            },
        )
        androidExtension.finalizeDsl {
            val outputConfig = buildPublishExtension.output.getDefault()!!
            val useDefaultVersionsAsFallback = outputConfig
                .useDefaultVersionsAsFallback
                .getOrElse(true)

            project.plugins.all { plugin ->
                when (plugin) {
                    is AppPlugin -> {
                        if (useDefaultVersionsAsFallback) {
                            val appExtension = project.extensions.getByType(AppExtension::class.java)
                            appExtension.configure()
                        }
                    }
                }
            }
        }
    }

    private fun Project.registerChangelogDependentTasks(
        changelogConfig: ChangelogConfig,
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        outputProviders: OutputProviders,
        grgitService: Property<GrgitService>,
        apkOutputFileProvider: Provider<RegularFile>,
        bundleFile: Provider<RegularFile>
    ) {
        val outputConfig = buildPublishExtension.output.getByNameOrRequiredDefault(buildVariant.name)

        val generateChangelogFileProvider =
            ChangelogTasksRegistrar.registerGenerateChangelogTask(
                project = this,
                params = GenerateChangelogTaskParams(
                    changelogConfig.commitMessageKey,
                    outputConfig.buildTagPattern,
                    buildVariant,
                    changelogFileProvider,
                    outputProviders.tagBuildProvider,
                    grgitService,
                )
            )

        buildPublishExtension.telegram.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                TelegramTasksRegistrar.registerChangelogTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = TelegramChangelogTaskParams(
                        outputConfig.baseFileName,
                        changelogConfig.issueNumberPattern,
                        changelogConfig.issueUrlPrefix,
                        buildVariant,
                        generateChangelogFileProvider,
                        outputProviders.tagBuildProvider,
                    )
                )
                TelegramTasksRegistrar.registerDistributionTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = TelegramDistributionTasksParams(
                        outputConfig.baseFileName,
                        buildVariant,
                        outputProviders.tagBuildProvider,
                        apkOutputFileProvider,
                    )
                )
            }

        buildPublishExtension.confluence.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                ConfluenceTasksRegistrar.registerDistributionTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = ConfluenceDistributionTaskParams(
                        buildVariant = buildVariant,
                        apkOutputFileProvider = apkOutputFileProvider,
                    )
                )
            }
        buildPublishExtension.slack.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                SlackTasksRegistrar.registerChangelogTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = SlackChangelogTaskParams(
                        outputConfig.baseFileName,
                        changelogConfig.issueNumberPattern,
                        changelogConfig.issueUrlPrefix,
                        buildVariant,
                        generateChangelogFileProvider,
                        outputProviders.tagBuildProvider,
                    )
                )
                SlackTasksRegistrar.registerDistributionTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = SlackDistributionTasksParams(
                        outputConfig.baseFileName,
                        buildVariant,
                        outputProviders.tagBuildProvider,
                        apkOutputFileProvider,
                    )
                )
            }

        buildPublishExtension.appCenterDistribution.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                AppCenterTasksRegistrar.registerDistributionTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = AppCenterDistributionTaskParams(
                        buildVariant = buildVariant,
                        changelogFileProvider = generateChangelogFileProvider,
                        apkOutputFileProvider = apkOutputFileProvider,
                        tagBuildProvider = outputProviders.tagBuildProvider,
                        baseFileName = outputConfig.baseFileName,
                    )
                )
            }

        buildPublishExtension.play.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                PlayTasksRegistrar.registerDistributionTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = PlayTaskParams(
                        buildVariant = buildVariant,
                        bundleOutputFileProvider = bundleFile,
                        tagBuildProvider = outputProviders.tagBuildProvider,
                    )
                )
            }

        buildPublishExtension.jira.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                JiraTasksRegistrar.registerAutomationTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = JiraAutomationTaskParams(
                        buildVariant = buildVariant,
                        issueNumberPattern = changelogConfig.issueNumberPattern,
                        changelogFileProvider = changelogFileProvider,
                        tagBuildProvider = outputProviders.tagBuildProvider,
                    )
                )
            }

        buildPublishExtension.clickUp.getByNameOrNullableDefault(buildVariant.name)
            ?.apply {
                ClickUpTasksRegistrar.registerAutomationTask(
                    project = this@registerChangelogDependentTasks.tasks,
                    config = this,
                    params = ClickUpAutomationTaskParams(
                        buildVariant = buildVariant,
                        issueNumberPattern = changelogConfig.issueNumberPattern,
                        changelogFileProvider = changelogFileProvider,
                        tagBuildProvider = outputProviders.tagBuildProvider,
                    )
                )
            }
    }
}

@Suppress("ThrowsCount") // block to throws exceptions on apply
private fun Project.stopExecutionIfNotSupported() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
        throw StopExecutionException(
            "Must only be used with with Android Gradle Plugin >= 7.4 ",
        )
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Android application projects." +
                " Please apply the 'com.android.application' plugin.",
        )
    }
}

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = DEFAULT_VERSION_CODE
        it.versionName = DEFAULT_VERSION_NAME
    }
}

private fun Project.mapToOutputApkFile(
    buildVariant: BuildVariant,
    fileName: String,
): Provider<RegularFile> {
    return tasks.withType(PackageAndroidArtifact::class.java)
        .firstOrNull { it.variantName == buildVariant.name }
        ?.outputDirectory
        ?.map { directory -> directory.file(fileName) }
        ?: throw GradleException("no output for variant ${buildVariant.name}")
}
