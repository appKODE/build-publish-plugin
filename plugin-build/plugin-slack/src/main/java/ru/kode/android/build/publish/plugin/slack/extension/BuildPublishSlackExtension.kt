package ru.kode.android.build.publish.plugin.slack.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.task.SlackApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import javax.inject.Inject

/**
 * Main extension class for configuring Slack integration in the build-publish plugin.
 *
 * This extension provides configuration options for Slack file uploads
 * as part of the Android build and publish process. It allows configuration of:
 * - Distribution uploads (sharing APK files via Slack with rich text changelog)
 *
 * @see SlackDistributionConfig For file distribution options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<SlackDistributionConfig> =
            objectFactory.domainObjectContainer(SlackDistributionConfig::class.java)

        /**
         * Retrieves a required distribution configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val distributionConfig: (buildName: String) -> SlackDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional distribution configuration for the specified build variant.
         * @return The distribution configuration or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> SlackDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures file distribution settings for specific build variants.
         *
         * @param configurationAction The action to configure the distribution settings
         * @see SlackDistributionConfig For available configuration options
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<SlackDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures common distribution settings that apply to all build variants.
         * These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The action to configure the common distribution settings
         */
        fun distributionCommon(configurationAction: Action<SlackDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures the Slack plugin for the given project and build variant.
         *
         * This internal method is called during the project configuration phase to set up
         * the necessary tasks and services for Slack notifications. It registers the
         * appropriate tasks based on the build variant and configuration.
         *
         * @param project The target project to configure
         * @param input Extension input containing build variant and configuration details
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val distributionConfig = distributionConfigOrNull(input.buildVariant.name)

            if (distributionConfig != null) {
                SlackTasksRegistrar.registerApkDistributionTask(
                    project = project,
                    distributionConfig = distributionConfig,
                    params =
                        SlackApkDistributionTaskParams(
                            baseFileName = input.output.baseFileName,
                            buildVariant = input.buildVariant,
                            lastBuildTagFile = input.output.lastBuildTagFile,
                            apkOutputFile = input.output.apkFile,
                            changelogFile = input.changelog.file,
                        ),
                )

                SlackTasksRegistrar.registerBundleDistributionTask(
                    project = project,
                    distributionConfig = distributionConfig,
                    params =
                        SlackBundleDistributionTaskParams(
                            baseFileName = input.output.baseFileName,
                            buildVariant = input.buildVariant,
                            lastBuildTagFile = input.output.lastBuildTagFile,
                            bundleOutputFile = input.output.bundleFile,
                            changelogFile = input.changelog.file,
                        ),
                )
            }
        }
    }
