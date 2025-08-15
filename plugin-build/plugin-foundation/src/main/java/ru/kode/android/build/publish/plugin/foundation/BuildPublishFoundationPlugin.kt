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
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.git.DEFAULT_TAG_PATTERN
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.core.util.getCommon
import ru.kode.android.build.publish.plugin.foundation.extension.BuildPublishFoundationExtension
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorServicePlugin
import ru.kode.android.build.publish.plugin.foundation.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.util.mapToOutputApkFile
import ru.kode.android.build.publish.plugin.foundation.validate.stopExecutionIfNotSupported

private const val EXTENSION_NAME = "buildPublishFoundation"

abstract class BuildPublishFoundationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        project.pluginManager.apply(GitExecutorServicePlugin::class.java)

        val buildPublishFoundationExtension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishFoundationExtension::class.java)
        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.onVariants(
            callback = { variant ->
                val variantOutput =
                    variant.outputs
                        .find { it is VariantOutputImpl && it.fullName == variant.name }
                        as? VariantOutputImpl

                if (variantOutput != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val apkOutputFileName = variantOutput.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)

                    val outputConfig =
                        buildPublishFoundationExtension
                            .output
                            .getByNameOrRequiredCommon(buildVariant.name)

                    val buildTagPattern =
                        outputConfig.buildTagPattern.orElse(
                            DEFAULT_TAG_PATTERN.format(buildVariant.name),
                        )

                    val lastTagTaskOutput =
                        TagTasksRegistrar.registerLastTagTask(
                            project,
                            params =
                                LastTagTaskParams(
                                    buildVariant = buildVariant,
                                    apkOutputFileName = apkOutputFileName,
                                    useVersionsFromTag =
                                        outputConfig.useVersionsFromTag
                                            .convention(true),
                                    baseFileName = outputConfig.baseFileName,
                                    useDefaultsForVersionsAsFallback =
                                        outputConfig.useDefaultsForVersionsAsFallback
                                            .convention(true),
                                    useStubsForTagAsFallback =
                                        outputConfig.useStubsForTagAsFallback
                                            .convention(true),
                                    buildTagPattern = buildTagPattern,
                                ),
                        )

                    TagTasksRegistrar.registerPrintLastIncreasedTagTask(
                        project = project,
                        params =
                            PrintLastIncreasedTagTaskParams(
                                buildVariant = buildVariant,
                                lastBuildTagFile = lastTagTaskOutput.lastBuildTagFile,
                            ),
                    )

                    val changelogConfig =
                        buildPublishFoundationExtension
                            .changelog
                            .getByNameOrNullableCommon(buildVariant.name)

                    if (changelogConfig != null) {
                        val apkOutputFile =
                            lastTagTaskOutput.apkOutputFileName.flatMap { fileName ->
                                project.mapToOutputApkFile(buildVariant, fileName)
                            }

                        val changelogFile =
                            ChangelogTasksRegistrar.registerGenerateChangelogTask(
                                project = project,
                                params =
                                    GenerateChangelogTaskParams(
                                        commitMessageKey = changelogConfig.commitMessageKey,
                                        buildTagPattern = buildTagPattern,
                                        buildVariant = buildVariant,
                                        changelogFile = project.changelogDirectory(),
                                        lastTagFile = lastTagTaskOutput.lastBuildTagFile,
                                    ),
                            )

                        project.extensions.extensionsSchema
                            .filter { schema ->
                                val extensionType: Class<*> = schema.publicType.concreteClass
                                BuildPublishConfigurableExtension::class.java.isAssignableFrom(extensionType)
                            }
                            .map { schema ->
                                project.extensions.getByName(schema.name) as BuildPublishConfigurableExtension
                            }
                            .onEach { extension ->
                                extension.configure(
                                    project = project,
                                    input =
                                        ExtensionInput(
                                            changelog =
                                                ExtensionInput.Changelog(
                                                    issueNumberPattern = changelogConfig.issueNumberPattern,
                                                    issueUrlPrefix = changelogConfig.issueUrlPrefix,
                                                    commitMessageKey = changelogConfig.commitMessageKey,
                                                    file = changelogFile,
                                                ),
                                            output =
                                                ExtensionInput.Output(
                                                    baseFileName = outputConfig.baseFileName,
                                                    buildTagPattern = outputConfig.buildTagPattern,
                                                    lastBuildTagFile = lastTagTaskOutput.lastBuildTagFile,
                                                    versionName = lastTagTaskOutput.versionName,
                                                    versionCode = lastTagTaskOutput.versionCode,
                                                    apkFileName = lastTagTaskOutput.apkOutputFileName,
                                                    apkFile = apkOutputFile,
                                                    bundleFile = bundleFile,
                                                ),
                                            buildVariant = buildVariant,
                                        ),
                                )
                            }
                    }

                    if (lastTagTaskOutput.versionCode != null) {
                        variantOutput.versionCode.set(lastTagTaskOutput.versionCode)
                    }
                    if (lastTagTaskOutput.versionName != null) {
                        variantOutput.versionName.set(lastTagTaskOutput.versionName)
                    }
                    variantOutput.outputFileName.set(lastTagTaskOutput.apkOutputFileName)
                }
            },
        )
        androidExtension.finalizeDsl {
            val outputConfig =
                buildPublishFoundationExtension.output.getCommon()
                    ?: throw GradleException("output config should be defined")
            val useDefaultVersionsAsFallback =
                outputConfig
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
