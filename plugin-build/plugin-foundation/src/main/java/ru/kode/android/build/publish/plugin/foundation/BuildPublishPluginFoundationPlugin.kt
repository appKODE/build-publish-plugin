@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.foundation

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.extensions.BuildPublishAppCenterExtension
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.clickup.extensions.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.confluence.extensions.BuildPublishConfluenceExtension
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.git.DEFAULT_TAG_PATTERN
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableDefault
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredDefault
import ru.kode.android.build.publish.plugin.core.util.getDefault
import ru.kode.android.build.publish.plugin.foundation.extension.BuildPublishBaseExtension
import ru.kode.android.build.publish.plugin.foundation.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.foundation.service.GitExecutorServicePlugin
import ru.kode.android.build.publish.plugin.foundation.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.OutputProviders
import ru.kode.android.build.publish.plugin.foundation.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.util.mapToOutputApkFile
import ru.kode.android.build.publish.plugin.foundation.validate.stopExecutionIfNotSupported
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
import ru.kode.android.build.publish.plugin.telegram.extensions.BuildPublishTelegramExtension
import ru.kode.android.build.publish.plugin.telegram.task.TelegramChangelogTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramDistributionTasksParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramTasksRegistrar

private const val EXTENSION_NAME = "buildPublishFoundation"

abstract class BuildPublishPluginFoundationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        project.pluginManager.apply(GitExecutorServicePlugin::class.java)

        val buildPublishBaseExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishBaseExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)

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

                    val buildTagPatternProvider = outputConfig.buildTagPattern.orElse(
                        DEFAULT_TAG_PATTERN.format(buildVariant.name)
                    )

                    val outputProviders = TagTasksRegistrar.registerLastTagTask(
                        project,
                        params = LastTagTaskParams(
                            buildVariant = buildVariant,
                            apkOutputFileName = apkOutputFileName,
                            useVersionsFromTag = outputConfig.useVersionsFromTag
                                .convention(true),
                            baseFileName = outputConfig.baseFileName,
                            useDefaultsForVersionsAsFallback = outputConfig.useDefaultsForVersionsAsFallback
                                .convention(true),
                            useStubsForTagAsFallback = outputConfig.useStubsForTagAsFallback
                                .convention(true),
                            buildTagPattern = buildTagPatternProvider
                        )
                    )

                    TagTasksRegistrar.registerPrintLastIncreasedTagTask(
                        project = project,
                        params = PrintLastIncreasedTagTaskParams(
                            buildVariant = buildVariant,
                            tagBuildProvider = outputProviders.tagBuildProvider,
                        )
                    )

                    val changelogConfig = buildPublishBaseExtension.changelog.getByNameOrNullableDefault(buildVariant.name)

                    if (changelogConfig != null) {
                        val apkOutputFileProvider = outputProviders.apkOutputFileName.flatMap { fileName ->
                            project.mapToOutputApkFile(buildVariant, fileName)
                        }

                        val generateChangelogFileProvider =
                            ChangelogTasksRegistrar.registerGenerateChangelogTask(
                                project = project,
                                params = GenerateChangelogTaskParams(
                                    commitMessageKey = changelogConfig.commitMessageKey,
                                    buildTagPatternProvider = buildTagPatternProvider,
                                    buildVariant = buildVariant,
                                    changelogFile = project.changelogDirectory(),
                                    tagBuildProvider = outputProviders.tagBuildProvider,
                                )
                            )

                        project.registerChangelogDependentTasks(
                            changelogConfig = changelogConfig,
                            outputConfig = outputConfig,
                            buildVariant = buildVariant,
                            generateChangelogFileProvider = generateChangelogFileProvider,
                            outputProviders = outputProviders,
                            apkOutputFileProvider = apkOutputFileProvider,
                            bundleFile = bundleFile,
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
                .useDefaultsForVersionsAsFallback
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
    generateChangelogFileProvider: Provider<RegularFile>,
    outputProviders: OutputProviders,
    apkOutputFileProvider: Provider<RegularFile>,
    bundleFile: Provider<RegularFile>
) {

    val telegramExtension = extensions.findByType(BuildPublishTelegramExtension::class.java)
    val issueNumberPattern = changelogConfig.issueNumberPattern

    telegramExtension?.let { extension ->
        val telegramBotConfig = extension.bot.getByNameOrRequiredDefault(buildVariant.name)
        val telegramChangelogConfig = extension.changelog.getByNameOrNullableDefault(buildVariant.name)
        val telegramDistributionConfig = extension.distribution.getByNameOrNullableDefault(buildVariant.name)

        if (telegramChangelogConfig != null) {
            TelegramTasksRegistrar.registerChangelogTask(
                project = this@registerChangelogDependentTasks.tasks,
                botConfig = telegramBotConfig,
                changelogConfig = telegramChangelogConfig,
                params = TelegramChangelogTaskParams(
                    baseFileName = outputConfig.baseFileName,
                    issueNumberPattern = issueNumberPattern,
                    issueUrlPrefix = changelogConfig.issueUrlPrefix,
                    buildVariant = buildVariant,
                    generateChangelogFileProvider = generateChangelogFileProvider,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                )
            )
        }
        if (telegramDistributionConfig != null) {
            TelegramTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks.tasks,
                botConfig = telegramBotConfig,
                distributionConfig = telegramDistributionConfig,
                params = TelegramDistributionTasksParams(
                    baseFileName = outputConfig.baseFileName,
                    buildVariant = buildVariant,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                    apkOutputFileProvider = apkOutputFileProvider,
                )
            )
        }
    }

    val confluenceExtension = extensions.findByType(BuildPublishConfluenceExtension::class.java)

    confluenceExtension?.let { extension ->
        val confluenceDistributionConfig = extension.distribution.getByNameOrRequiredDefault(buildVariant.name)
        ConfluenceTasksRegistrar.registerDistributionTask(
            project = this@registerChangelogDependentTasks,
            distributionConfig = confluenceDistributionConfig,
            params = ConfluenceDistributionTaskParams(
                buildVariant = buildVariant,
                apkOutputFileProvider = apkOutputFileProvider,
            )
        )
    }

    val slackExtension = extensions.findByType(BuildPublishSlackExtension::class.java)

    slackExtension?.let { extension ->
        val slackBotConfig = extension.bot.getByNameOrRequiredDefault(buildVariant.name)
        val slackChangelogConfig = extension.changelog.getByNameOrNullableDefault(buildVariant.name)
        val slackDistributionConfig = extension.distribution.getByNameOrNullableDefault(buildVariant.name)

        if (slackChangelogConfig != null) {
            SlackTasksRegistrar.registerChangelogTask(
                project = this@registerChangelogDependentTasks.tasks,
                botConfig = slackBotConfig,
                changelogConfig = slackChangelogConfig,
                params = SlackChangelogTaskParams(
                    baseFileName = outputConfig.baseFileName,
                    issueNumberPattern = issueNumberPattern,
                    issueUrlPrefix = changelogConfig.issueUrlPrefix,
                    buildVariant = buildVariant,
                    generateChangelogFileProvider = generateChangelogFileProvider,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                )
            )
        }

        if (slackDistributionConfig != null) {
            SlackTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks.tasks,
                distributionConfig = slackDistributionConfig,
                params = SlackDistributionTasksParams(
                    baseFileName = outputConfig.baseFileName,
                    buildVariant = buildVariant,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                    apkOutputFileProvider = apkOutputFileProvider,
                )
            )
        }
    }

    val appCenterExtension = extensions.findByType(BuildPublishAppCenterExtension::class.java)

    appCenterExtension?.let { extension ->
        val appCenterAuthConfig = extension.auth.getByNameOrRequiredDefault(buildVariant.name)
        val appCenterDistributionConfig = extension.distribution.getByNameOrRequiredDefault(buildVariant.name)

        AppCenterTasksRegistrar.registerDistributionTask(
            project = this@registerChangelogDependentTasks,
            distributionConfig = appCenterDistributionConfig,
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

    playExtension?.let { extension ->
        val playDistributionConfig = extension.distribution.getByNameOrRequiredDefault(buildVariant.name)
        PlayTasksRegistrar.registerDistributionTask(
            project = this@registerChangelogDependentTasks.tasks,
            distributionConfig = playDistributionConfig,
            params = PlayTaskParams(
                buildVariant = buildVariant,
                bundleOutputFileProvider = bundleFile,
                tagBuildProvider = outputProviders.tagBuildProvider,
            )
        )
    }

    val jiraExtension = extensions.findByType(BuildPublishJiraExtension::class.java)

    jiraExtension?.let { extension ->
        val jiraAutomationConfig = extension.automation.getByNameOrRequiredDefault(buildVariant.name)
        JiraTasksRegistrar.registerAutomationTask(
            project = this@registerChangelogDependentTasks,
            automationConfig = jiraAutomationConfig,
            params = JiraAutomationTaskParams(
                buildVariant = buildVariant,
                issueNumberPattern = issueNumberPattern,
                changelogFileProvider = generateChangelogFileProvider,
                tagBuildProvider = outputProviders.tagBuildProvider,
            )
        )
    }

    val clickUpExtension = extensions.findByType(BuildPublishClickUpExtension::class.java)

    clickUpExtension?.let { extension ->
        val clickUpAutomationConfig = extension.automation.getByNameOrRequiredDefault(buildVariant.name)

        ClickUpTasksRegistrar.registerAutomationTask(
            project = this@registerChangelogDependentTasks,
            automationConfig = clickUpAutomationConfig,
            params = ClickUpAutomationTaskParams(
                buildVariant = buildVariant,
                issueNumberPattern = issueNumberPattern,
                changelogFileProvider = generateChangelogFileProvider,
                tagBuildProvider = outputProviders.tagBuildProvider,
            )
        )
    }
}
