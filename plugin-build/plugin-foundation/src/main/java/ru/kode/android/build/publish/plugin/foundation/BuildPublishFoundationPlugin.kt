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
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.changelogDirectory
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.core.util.getCommon
import ru.kode.android.build.publish.plugin.foundation.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.config.OutputConfig
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
                val variantName = variant.name
                val buildVariant = BuildVariant(
                    variantName,
                    variant.flavorName,
                    variant.buildType
                )

                val variantOutput =
                    variant.outputs
                        .find { it is VariantOutputImpl && it.fullName == variantName }
                        as? VariantOutputImpl

                if (variantOutput != null) {
                    // It's safe to call `.get()` here because `variantOutput.outputFileName` is already
                    // resolved by Gradle during the variant configuration phase. We are not creating a new
                    // lazy Provider, just accessing the current value. This avoids a circular dependency
                    // that would occur if we tried to build a flatMap/zip based on itself.
                    val apkOutputFileName = variantOutput.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)

                    val outputConfigProvider: Provider<OutputConfig> =
                        project.providers.provider {
                            buildPublishFoundationExtension
                                .output
                                .getByNameOrRequiredCommon(buildVariant.name)
                        }

                    val buildTagPattern = outputConfigProvider.flatMap { outputConfig ->
                        outputConfig.buildTagPattern
                            .orElse(DEFAULT_TAG_PATTERN)
                            .map { it.format(buildVariant.name) }
                    }

                    val lastTagTaskOutput =
                        TagTasksRegistrar.registerLastTagTask(
                            project,
                            params =
                                LastTagTaskParams(
                                    buildVariant = buildVariant,
                                    apkOutputFileName = project.providers.provider {
                                        apkOutputFileName
                                    },
                                    useVersionsFromTag = outputConfigProvider
                                        .flatMap { it.useVersionsFromTag }
                                        .orElse(true),
                                    baseFileName = outputConfigProvider
                                        .flatMap { it.baseFileName },
                                    useDefaultsForVersionsAsFallback =
                                        outputConfigProvider
                                            .flatMap { it.useDefaultsForVersionsAsFallback }
                                            .orElse(true),
                                    useStubsForTagAsFallback =
                                        outputConfigProvider
                                            .flatMap { it.useStubsForTagAsFallback }
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


                    val changelogConfigProvider: Provider<ChangelogConfig?> =
                        project.providers.provider {
                            buildPublishFoundationExtension
                                .changelog
                                .getByNameOrNullableCommon(buildVariant.name)
                        }

                    val apkOutputFile =
                        lastTagTaskOutput.apkOutputFileName.flatMap { fileName ->
                            project.mapToOutputApkFile(buildVariant, fileName)
                        }

                    val changelogFile =
                        ChangelogTasksRegistrar.registerGenerateChangelogTask(
                            project = project,
                            params =
                                GenerateChangelogTaskParams(
                                    commitMessageKey = changelogConfigProvider
                                        .flatMap {
                                            it?.commitMessageKey
                                                ?: project.providers.provider { null }
                                        },
                                    excludeMessageKey = changelogConfigProvider.flatMap {
                                        if (it != null) {
                                            it.excludeMessageKey
                                                .orElse(true)
                                        } else {
                                            project.providers.provider { null }
                                        }
                                    },
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
                                                issueNumberPattern = changelogConfigProvider.flatMap {
                                                    it?.issueNumberPattern
                                                        ?: project.providers.provider { null }
                                                },
                                                issueUrlPrefix = changelogConfigProvider.flatMap {
                                                    it?.issueUrlPrefix
                                                        ?: project.providers.provider { null }
                                                },
                                                commitMessageKey = changelogConfigProvider.flatMap {
                                                    it?.commitMessageKey
                                                        ?: project.providers.provider { null }
                                                },
                                                file = changelogFile,
                                            ),
                                        output =
                                            ExtensionInput.Output(
                                                baseFileName = outputConfigProvider
                                                    .flatMap { it.baseFileName },
                                                buildTagPattern = outputConfigProvider
                                                    .flatMap { it.buildTagPattern },
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
