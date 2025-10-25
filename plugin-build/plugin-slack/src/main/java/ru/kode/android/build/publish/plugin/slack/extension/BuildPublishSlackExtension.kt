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
import ru.kode.android.build.publish.plugin.slack.config.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.task.SlackApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackChangelogTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import javax.inject.Inject

/**
 * Main extension class for configuring Slack integration in the build-publish plugin.
 *
 * This extension provides configuration options for Slack notifications and file uploads
 * as part of the Android build and publish process. It allows configuration of:
 * - Bot configurations (authentication and connection details)
 * - Changelog notifications (sending release notes to Slack)
 * - Distribution uploads (sharing APK files via Slack)
 *
 * @see SlackBotConfig For bot configuration options
 * @see SlackChangelogConfig For changelog notification options
 * @see SlackDistributionConfig For file distribution options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for bot configurations, keyed by build type.
         *
         * This internal property holds all the bot configurations for different build types.
         * Use the [bot] and [botCommon] methods to configure these settings in your build script.
         */
        internal val bot: NamedDomainObjectContainer<SlackBotConfig> =
            objectFactory.domainObjectContainer(SlackBotConfig::class.java)

        /**
         * Container for changelog configurations, keyed by build type.
         *
         * This internal property holds all the changelog configurations for different build types.
         * Use the [changelog] and [changelogCommon] methods to configure these settings in your build script.
         */
        internal val changelog: NamedDomainObjectContainer<SlackChangelogConfig> =
            objectFactory.domainObjectContainer(SlackChangelogConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<SlackDistributionConfig> =
            objectFactory.domainObjectContainer(SlackDistributionConfig::class.java)

        /**
         * Retrieves a required bot configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val botConfig: (buildName: String) -> SlackBotConfig = { buildName ->
            bot.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional bot configuration for the specified build variant.
         * @return The bot configuration or null if not found
         */
        val botConfigOrNull: (buildName: String) -> SlackBotConfig? = { buildName ->
            bot.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves a required changelog configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val changelogConfig: (buildName: String) -> SlackChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional changelog configuration for the specified build variant.
         * @return The changelog configuration or null if not found
         */
        val changelogConfigOrNull: (buildName: String) -> SlackChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

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
         * Configures bot settings for specific build variants.
         *
         * @param configurationAction The action to configure the bot settings
         * @see SlackBotConfig For available configuration options
         */
        fun bot(configurationAction: Action<BuildPublishDomainObjectContainer<SlackBotConfig>>) {
            val container = BuildPublishDomainObjectContainer(bot)
            configurationAction.execute(container)
        }

        /**
         * Configures changelog notifications for specific build variants.
         *
         * @param configurationAction The action to configure the changelog settings
         * @see SlackChangelogConfig For available configuration options
         */
        fun changelog(configurationAction: Action<BuildPublishDomainObjectContainer<SlackChangelogConfig>>) {
            val container = BuildPublishDomainObjectContainer(changelog)
            configurationAction.execute(container)
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
         * Configures common bot settings that apply to all build variants.
         * These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The action to configure the common bot settings
         */
        fun botCommon(configurationAction: Action<SlackBotConfig>) {
            common(bot, configurationAction)
        }

        /**
         * Configures common changelog settings that apply to all build variants.
         * These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The action to configure the common changelog settings
         */
        fun changelogCommon(configurationAction: Action<SlackChangelogConfig>) {
            common(changelog, configurationAction)
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
            val botConfig = botConfig(input.buildVariant.name)
            val changelogConfig = changelogConfigOrNull(input.buildVariant.name)
            val distributionConfig = distributionConfigOrNull(input.buildVariant.name)

            if (changelogConfig != null) {
                SlackTasksRegistrar.registerChangelogTask(
                    project = project,
                    botConfig = botConfig,
                    changelogConfig = changelogConfig,
                    params =
                        SlackChangelogTaskParams(
                            baseFileName = input.output.baseFileName,
                            issueNumberPattern = input.changelog.issueNumberPattern,
                            issueUrlPrefix = input.changelog.issueUrlPrefix,
                            buildVariant = input.buildVariant,
                            changelogFile = input.changelog.file,
                            lastBuildTagFile = input.output.lastBuildTagFile,
                        ),
                )
            }

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
                        ),
                )
            }
        }
    }
