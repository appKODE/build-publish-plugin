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
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.logger.LOGGER_SERVICE_EXTENSION_NAME
import ru.kode.android.build.publish.plugin.core.logger.LOGGER_SERVICE_NAME
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_TAG_PATTERN
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.util.changelogFileProvider
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.core.util.getCommon
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.config.OutputConfig
import ru.kode.android.build.publish.plugin.foundation.extension.BuildPublishFoundationExtension
import ru.kode.android.build.publish.plugin.foundation.messages.configureExtensionMessage
import ru.kode.android.build.publish.plugin.foundation.messages.outputConfigShouldBeDefinedMessage
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorServicePlugin
import ru.kode.android.build.publish.plugin.foundation.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.task.GenerateChangelogTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.LastTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.PrintLastIncreasedTagTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.RenameApkTaskParams
import ru.kode.android.build.publish.plugin.foundation.task.TagTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.rename.RenameApkTask
import ru.kode.android.build.publish.plugin.foundation.validate.stopExecutionIfNotSupported

const val EXTENSION_NAME = "buildPublishFoundation"

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
    @Suppress("LongMethod") // Just big creation methods
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        val buildPublishFoundationExtension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishFoundationExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        val loggerServiceProvider =
            project.gradle.sharedServices.registerIfAbsent(
                project.serviceName(LOGGER_SERVICE_NAME),
                LoggerService::class.java,
            ) {
                it.parameters.verboseLogging.set(
                    buildPublishFoundationExtension.verboseLogging,
                )
                it.parameters.bodyLogging.set(
                    buildPublishFoundationExtension.bodyLogging,
                )
            }

        project.extensions.create(
            LOGGER_SERVICE_EXTENSION_NAME,
            LoggerServiceExtension::class.java,
            loggerServiceProvider,
        )

        project.pluginManager.apply(GitExecutorServicePlugin::class.java)

        androidExtension.onVariants(
            callback = { variant ->
                val logger =
                    project.extensions
                        .getByType(LoggerServiceExtension::class.java)
                        .service
                        .get()

                val buildVariant =
                    BuildVariant(
                        name = variant.name,
                        flavorName = variant.flavorName?.takeIf { it.isNotBlank() },
                        buildTypeName = variant.buildType?.takeIf { it.isNotBlank() },
                        productFlavors =
                            variant.productFlavors.map {
                                BuildVariant.ProductFlavor(
                                    dimension = it.first,
                                    name = it.second,
                                )
                            },
                    )

                val variantOutput =
                    variant.outputs
                        .find { it is VariantOutputImpl && it.fullName == buildVariant.name }
                        as? VariantOutputImpl

                if (variantOutput != null) {
                    val outputConfigProvider: Provider<OutputConfig> =
                        project.providers.provider {
                            buildPublishFoundationExtension
                                .output
                                .getByNameOrRequiredCommon(buildVariant.name)
                        }

                    val buildTagPattern =
                        outputConfigProvider.flatMap { outputConfig ->
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
                                    apkOutputFileName = variantOutput.outputFileName,
                                    useVersionsFromTag =
                                        outputConfigProvider
                                            .flatMap { it.useVersionsFromTag }
                                            .orElse(true),
                                    baseFileName =
                                        outputConfigProvider
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
                                    versionNameStrategy =
                                        outputConfigProvider
                                            .flatMap { it.versionNameStrategy },
                                    versionCodeStrategy =
                                        outputConfigProvider
                                            .flatMap { it.versionCodeStrategy },
                                    outputApkNameStrategy =
                                        outputConfigProvider
                                            .flatMap { it.outputApkNameStrategy },
                                ),
                        )

                    val apkDirProvider = variant.artifacts.get(SingleArtifact.APK)

                    val renameApkTaskProvider =
                        TagTasksRegistrar.registerRenameApkTask(
                            project,
                            RenameApkTaskParams(
                                inputDir = apkDirProvider,
                                buildVariant = buildVariant,
                                outputFileName = lastTagTaskOutput.apkOutputFileName,
                            ),
                        )

                    val renameTransformationRequest =
                        variant.artifacts.use(renameApkTaskProvider)
                            .wiredWithDirectories(
                                RenameApkTask::inputDir,
                                RenameApkTask::outputDir,
                            )
                            .toTransformMany(SingleArtifact.APK)

                    renameApkTaskProvider.configure { it.transformationRequest.set(renameTransformationRequest) }

                    val bundleFileProvider = variant.artifacts.get(SingleArtifact.BUNDLE)

                    TagTasksRegistrar.registerPrintLastIncreasedTagTask(
                        project = project,
                        params =
                            PrintLastIncreasedTagTaskParams(
                                buildVariant = buildVariant,
                                lastBuildTagFile = lastTagTaskOutput.lastBuildTagFile,
                            ),
                    )

                    val changelogConfigProvider: Provider<ChangelogConfig> =
                        project.providers.provider {
                            buildPublishFoundationExtension
                                .changelog
                                .getByNameOrNullableCommon(buildVariant.name)
                        }

                    val apkOutputFileProvider: Provider<RegularFile> =
                        lastTagTaskOutput.apkOutputFileName
                            .zip(renameApkTaskProvider.flatMap { it.outputDir }) { outputFileName, outputDir ->
                                outputDir.file(outputFileName)
                            }

                    val changelogFileProvider = project.changelogFileProvider(buildVariant.name)

                    val changelogFile =
                        ChangelogTasksRegistrar.registerGenerateChangelogTask(
                            project = project,
                            params =
                                GenerateChangelogTaskParams(
                                    commitMessageKey =
                                        changelogConfigProvider
                                            .flatMap {
                                                it.commitMessageKey
                                                    .orElse(project.providers.provider { null })
                                            },
                                    excludeMessageKey =
                                        changelogConfigProvider.flatMap {
                                            it.excludeMessageKey.orElse(true)
                                        },
                                    buildTagPattern = buildTagPattern,
                                    buildVariant = buildVariant,
                                    changelogFile = changelogFileProvider,
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
                            logger.info(
                                configureExtensionMessage(
                                    extension.toString(),
                                    variant.name,
                                ),
                            )
                            extension.configure(
                                project = project,
                                input =
                                    ExtensionInput(
                                        changelog =
                                            ExtensionInput.Changelog(
                                                issueNumberPattern =
                                                    changelogConfigProvider.flatMap {
                                                        it.issueNumberPattern
                                                            .orElse(project.providers.provider { null })
                                                    },
                                                issueUrlPrefix =
                                                    changelogConfigProvider.flatMap {
                                                        it.issueUrlPrefix
                                                            .orElse(project.providers.provider { null })
                                                    },
                                                commitMessageKey =
                                                    changelogConfigProvider.flatMap {
                                                        it.commitMessageKey
                                                            .orElse(project.providers.provider { null })
                                                    },
                                                file = changelogFile,
                                            ),
                                        output =
                                            ExtensionInput.Output(
                                                baseFileName =
                                                    outputConfigProvider
                                                        .flatMap { it.baseFileName },
                                                buildTagPattern =
                                                    outputConfigProvider
                                                        .flatMap { it.buildTagPattern },
                                                lastBuildTagFile = lastTagTaskOutput.lastBuildTagFile,
                                                versionName = lastTagTaskOutput.versionName,
                                                versionCode = lastTagTaskOutput.versionCode,
                                                changelogFileName = changelogFile,
                                                apkFile = apkOutputFileProvider,
                                                bundleFile = bundleFileProvider,
                                            ),
                                        buildVariant = buildVariant,
                                    ),
                                variant = variant,
                            )
                        }
                    if (lastTagTaskOutput.versionCode.isPresent) {
                        variantOutput.versionCode.set(lastTagTaskOutput.versionCode)
                    }
                    if (lastTagTaskOutput.versionName.isPresent) {
                        variantOutput.versionName.set(lastTagTaskOutput.versionName)
                    }
                }
            },
        )
        androidExtension.finalizeDsl {
            val outputConfig =
                buildPublishFoundationExtension.output.getCommon()
                    ?: throw GradleException(outputConfigShouldBeDefinedMessage())
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
