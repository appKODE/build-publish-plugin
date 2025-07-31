@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.base

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.ajoberstar.grgit.gradle.GrgitService
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.extensions.BuildPublishAppCenterExtension
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.base.check.stopExecutionIfNotSupported
import ru.kode.android.build.publish.plugin.clickup.extensions.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.confluence.extensions.BuildPublishConfluenceExtension
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableDefault
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredDefault
import ru.kode.android.build.publish.plugin.core.util.getDefault
import ru.kode.android.build.publish.plugin.base.extension.BuildPublishBaseExtension
import ru.kode.android.build.publish.plugin.base.extension.BASE_EXTENSION_NAME
import ru.kode.android.build.publish.plugin.base.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.base.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.jira.extensions.BuildPublishJiraExtension
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import ru.kode.android.build.publish.plugin.play.extensions.BuildPublishPlayExtension
import ru.kode.android.build.publish.plugin.play.task.PlayTaskParams
import ru.kode.android.build.publish.plugin.play.task.PlayTasksRegistrar
import ru.kode.android.build.publish.plugin.slack.extensions.BuildPublishSlackExtension
import ru.kode.android.build.publish.plugin.slack.task.SlackChangelogTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackDistributionTasksParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import ru.kode.android.build.publish.plugin.base.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.base.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.base.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.base.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.base.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.base.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.base.task.OutputProviders
import ru.kode.android.build.publish.plugin.base.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.base.util.mapToOutputApkFile
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.telegram.extensions.BuildPublishTelegramExtension
import ru.kode.android.build.publish.plugin.telegram.task.TelegramChangelogTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramDistributionTasksParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramTasksRegistrar

abstract class BuildPublishPluginBase : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val buildPublishBaseExtension = project.extensions
            .create(BASE_EXTENSION_NAME, BuildPublishBaseExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFileProvider = project.changelogDirectory()
        val grgitService = project.extensions
            .getByType(GrgitServiceExtension::class.java)
            .service

        androidExtension.onVariants(
            callback = { variant ->
                val variantOutput = variant.outputs
                    .find { it is VariantOutputImpl && it.fullName == variant.name }
                    as? VariantOutputImpl

                if (variantOutput != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val apkOutputFileName = variantOutput.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)

                    val outputConfig = buildPublishBaseExtension.output.getByNameOrRequiredDefault(buildVariant.name)

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

                    val changelogConfig = buildPublishBaseExtension.changelog.getByNameOrNullableDefault(buildVariant.name)

                    if (changelogConfig != null) {
                        val apkOutputFileProvider = outputProviders.apkOutputFileName.flatMap { fileName ->
                            project.mapToOutputApkFile(buildVariant, fileName)
                        }

                        project.registerChangelogDependentTasks(
                            changelogConfig,
                            outputConfig,
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
            val outputConfig = buildPublishBaseExtension.output.getDefault()
                ?: throw GradleException("output config should be defined")
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

}

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = DEFAULT_VERSION_CODE
        it.versionName = DEFAULT_VERSION_NAME
    }
}

private fun Project.registerChangelogDependentTasks(
    changelogConfig: ChangelogConfig,
    outputConfig: OutputConfig,
    buildVariant: BuildVariant,
    changelogFileProvider: Provider<RegularFile>,
    outputProviders: OutputProviders,
    grgitService: Property<GrgitService>,
    apkOutputFileProvider: Provider<RegularFile>,
    bundleFile: Provider<RegularFile>
) {

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

    val telegramExtension = extensions.findByType(BuildPublishTelegramExtension::class.java)

    telegramExtension?.telegram?.getByNameOrNullableDefault(buildVariant.name)
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

    val confluenceExtension = extensions.findByType(BuildPublishConfluenceExtension::class.java)

    confluenceExtension?.confluence?.getByNameOrNullableDefault(buildVariant.name)
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

    val slackExtension = extensions.findByType(BuildPublishSlackExtension::class.java)

    slackExtension?.slack?.getByNameOrNullableDefault(buildVariant.name)
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

    val appCenterExtension = extensions.findByType(BuildPublishAppCenterExtension::class.java)

    appCenterExtension?.appCenterDistribution?.getByNameOrNullableDefault(buildVariant.name)
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

    val playExtension = extensions.findByType(BuildPublishPlayExtension::class.java)

    playExtension?.play?.getByNameOrNullableDefault(buildVariant.name)
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

    val jiraExtension = extensions.findByType(BuildPublishJiraExtension::class.java)

    jiraExtension?.jira?.getByNameOrNullableDefault(buildVariant.name)
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

    val clickUpExtension = extensions.findByType(BuildPublishClickUpExtension::class.java)

    clickUpExtension?.clickUp?.getByNameOrNullableDefault(buildVariant.name)
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
