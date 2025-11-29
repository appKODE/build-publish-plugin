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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.core.util.getCommon
import ru.kode.android.build.publish.plugin.foundation.extension.BuildPublishFoundationExtension
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorServicePlugin
import ru.kode.android.build.publish.plugin.foundation.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_PATTERN
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.util.mapToOutputApkFile
import ru.kode.android.build.publish.plugin.foundation.validate.stopExecutionIfNotSupported

private const val EXTENSION_NAME = "buildPublishFoundation"

/**
 * Core plugin that provides foundation functionality for the build publishing system.
 *
 * This plugin handles:
 * - Version management through Git tags
 * - Changelog generation
 * - Build variant configuration
 * - Integration with other build publishing plugins
 * - Common utilities and extensions for build variants
 *
 * It applies the [GitExecutorServicePlugin] and sets up the necessary task graph
 * for version management and changelog generation based on Git history.
 */
abstract class BuildPublishFoundationPlugin : Plugin<Project> {

    private val logger: Logger = Logging.getLogger(this::class.java)

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
                        outputConfig.buildTagPattern
                            .orElse(DEFAULT_TAG_PATTERN)
                            .map { it.format(buildVariant.name) }

                    val lastTagTaskOutput =
                        TagTasksRegistrar.registerLastTagTask(
                            project,
                            params =
                                LastTagTaskParams(
                                    buildVariant = buildVariant,
                                    apkOutputFileName = apkOutputFileName,
                                    useVersionsFromTag =
                                        outputConfig.useVersionsFromTag
                                            .orElse(true),
                                    baseFileName = outputConfig.baseFileName,
                                    useDefaultsForVersionsAsFallback =
                                        outputConfig.useDefaultsForVersionsAsFallback
                                            .orElse(true),
                                    useStubsForTagAsFallback =
                                        outputConfig.useStubsForTagAsFallback
                                            .orElse(true),
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
                                        excludeMessageKey =
                                            changelogConfig.excludeMessageKey
                                                .orElse(true),
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
                                logger.info("Configure $extension in core")

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

                    if (lastTagTaskOutput.versionCode.isPresent) {
                        variantOutput.versionCode.set(lastTagTaskOutput.versionCode)
                    }
                    if (lastTagTaskOutput.versionName.isPresent) {
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
            val useDefaultsForVersionsAsFallback =
                outputConfig
                    .useDefaultsForVersionsAsFallback
                    .getOrElse(true)

            project.plugins.all { plugin ->
                when (plugin) {
                    is AppPlugin -> {
                        if (useDefaultsForVersionsAsFallback) {
                            val appExtension = project.extensions.getByType(AppExtension::class.java)
                            appExtension.configure()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configures the Android application extension with default version information.
 *
 * Sets default versionCode and versionName if not explicitly configured.
 * These values are used as fallbacks when version information cannot be determined from Git tags.
 *
 * @receiver The Android application extension to configure
 */
private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = DEFAULT_VERSION_CODE
        it.versionName = DEFAULT_VERSION_NAME
    }
}
