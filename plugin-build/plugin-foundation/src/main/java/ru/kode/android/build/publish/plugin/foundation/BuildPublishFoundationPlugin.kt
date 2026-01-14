@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.foundation

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
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
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.config.OutputConfig
import ru.kode.android.build.publish.plugin.foundation.extension.BuildPublishFoundationExtension
import ru.kode.android.build.publish.plugin.foundation.messages.configureExtensionMessage
import ru.kode.android.build.publish.plugin.foundation.messages.resolvedOutputConfig
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorServicePlugin
import ru.kode.android.build.publish.plugin.foundation.task.ChangelogTasksRegistrar
import ru.kode.android.build.publish.plugin.foundation.task.ComputeApkOutputFileNameParams
import ru.kode.android.build.publish.plugin.foundation.task.ComputeVersionCodeParams
import ru.kode.android.build.publish.plugin.foundation.task.ComputeVersionNameParams
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

        val androidDsl =
            project.extensions.getByType(ApplicationExtension::class.java)

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
                        defaultVersionCode = androidDsl.defaultConfig.versionCode,
                        defaultVersionName = androidDsl.defaultConfig.versionName,
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
                                .also {
                                    logger.info(
                                        resolvedOutputConfig(it.name, buildVariant.name),
                                    )
                                }
                        }

                    val buildTagPattern =
                        outputConfigProvider.flatMap { outputConfig ->
                            outputConfig.buildTagPattern
                                .orElse(DEFAULT_TAG_PATTERN)
                                .map { it.format(buildVariant.name) }
                        }

                    val lastBuildTagProvider =
                        TagTasksRegistrar.registerGetLastTagTask(
                            project,
                            params =
                                LastTagTaskParams(
                                    buildVariant = buildVariant,
                                    useStubsForTagAsFallback =
                                        outputConfigProvider
                                            .flatMap { it.useStubsForTagAsFallback }
                                            .orElse(true),
                                    buildTagPattern = buildTagPattern,
                                ),
                        )

                    val apkOutputFileNameProvider =
                        TagTasksRegistrar.registerComputeApkOutputFileNameTask(
                            project,
                            params =
                                ComputeApkOutputFileNameParams(
                                    buildVariant = buildVariant,
                                    apkOutputFileName = variantOutput.outputFileName,
                                    useVersionsFromTag =
                                        outputConfigProvider
                                            .flatMap { it.useVersionsFromTag }
                                            .orElse(true),
                                    baseFileName =
                                        outputConfigProvider
                                            .flatMap { it.baseFileName },
                                    outputApkNameStrategy =
                                        outputConfigProvider
                                            .flatMap { it.outputApkNameStrategy },
                                    lastBuildTagProvider = lastBuildTagProvider,
                                ),
                        )

                    val versionCodeProvider =
                        TagTasksRegistrar.registerComputeVersionCodeTask(
                            project,
                            params =
                                ComputeVersionCodeParams(
                                    buildVariant = buildVariant,
                                    useVersionsFromTag =
                                        outputConfigProvider
                                            .flatMap { it.useVersionsFromTag }
                                            .orElse(true),
                                    useDefaultsForVersionsAsFallback =
                                        outputConfigProvider
                                            .flatMap { it.useDefaultsForVersionsAsFallback }
                                            .orElse(true),
                                    versionCodeStrategy =
                                        outputConfigProvider
                                            .flatMap { it.versionCodeStrategy },
                                    lastBuildTagProvider = lastBuildTagProvider,
                                ),
                        )

                    val versionNameProvider =
                        TagTasksRegistrar.registerComputeVersionNameTask(
                            project,
                            params =
                                ComputeVersionNameParams(
                                    buildVariant = buildVariant,
                                    useVersionsFromTag =
                                        outputConfigProvider
                                            .flatMap { it.useVersionsFromTag }
                                            .orElse(true),
                                    useDefaultsForVersionsAsFallback =
                                        outputConfigProvider
                                            .flatMap { it.useDefaultsForVersionsAsFallback }
                                            .orElse(true),
                                    versionNameStrategy =
                                        outputConfigProvider
                                            .flatMap { it.versionNameStrategy },
                                    lastBuildTagProvider = lastBuildTagProvider,
                                ),
                        )

                    val apkDirProvider = variant.artifacts.get(SingleArtifact.APK)

                    val apkOutputFileName =
                        apkOutputFileNameProvider
                            .flatMap { provider ->
                                provider.apkOutputFileNameFile.map {
                                    it.asFile.readText()
                                }
                            }

                    val renameApkTaskProvider =
                        TagTasksRegistrar.registerRenameApkTask(
                            project,
                            RenameApkTaskParams(
                                inputDir = apkDirProvider,
                                buildVariant = buildVariant,
                                outputFileName = apkOutputFileName,
                            ),
                        )

                    val renameApkTransformationRequest =
                        variant.artifacts.use(renameApkTaskProvider)
                            .wiredWithDirectories(
                                RenameApkTask::inputDir,
                                RenameApkTask::outputDir,
                            )
                            .toTransformMany(SingleArtifact.APK)

                    renameApkTaskProvider.configure { it.transformationRequest.set(renameApkTransformationRequest) }

                    TagTasksRegistrar.registerPrintLastIncreasedTagTask(
                        project = project,
                        params =
                            PrintLastIncreasedTagTaskParams(
                                buildVariant = buildVariant,
                                lastBuildTagFileProvider = lastBuildTagProvider,
                            ),
                    )

                    val changelogConfigProvider: Provider<ChangelogConfig> =
                        project.providers.provider {
                            buildPublishFoundationExtension
                                .changelog
                                .getByNameOrNullableCommon(buildVariant.name)
                        }

                    val changelogFileProvider =
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
                                    lastBuildTagFileProvider = lastBuildTagProvider,
                                ),
                        )

                    val apkFileProvider: Provider<RegularFile> =
                        apkOutputFileName
                            .zip(renameApkTaskProvider.flatMap { it.outputDir }) { outputFileName, outputDir ->
                                outputDir.file(outputFileName)
                            }

                    val bundleFileProvider = variant.artifacts.get(SingleArtifact.BUNDLE)

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
                                                fileProvider = changelogFileProvider,
                                            ),
                                        output =
                                            ExtensionInput.Output(
                                                baseFileName =
                                                    outputConfigProvider
                                                        .flatMap { it.baseFileName },
                                                buildTagPattern =
                                                    outputConfigProvider
                                                        .flatMap { it.buildTagPattern },
                                                lastBuildTagFileProvider = lastBuildTagProvider,
                                                apkFile = apkFileProvider,
                                                bundleFile = bundleFileProvider,
                                            ),
                                        buildVariant = buildVariant,
                                    ),
                                variant = variant,
                            )
                        }

                    variantOutput.versionCode.set(
                        versionCodeProvider.flatMap { provider ->
                            provider.versionCodeFile.map { it.asFile.readText().toIntOrNull() }
                        },
                    )
                    variantOutput.versionName.set(
                        versionNameProvider.flatMap { provider ->
                            provider.versionNameFile.map { it.asFile.readText() }
                        },
                    )
                }
            },
        )
    }
}
