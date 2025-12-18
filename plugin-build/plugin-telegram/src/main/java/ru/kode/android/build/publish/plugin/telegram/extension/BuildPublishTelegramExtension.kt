package ru.kode.android.build.publish.plugin.telegram.extension

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotsConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramDistributionConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramLookupConfig
import ru.kode.android.build.publish.plugin.telegram.messages.needToProvideBotsConfigMessage
import ru.kode.android.build.publish.plugin.telegram.messages.needToProvideChangelogOrDistributionConfigMessage
import ru.kode.android.build.publish.plugin.telegram.task.TelegramApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramChangelogTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramLookupTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramTasksRegistrar
import javax.inject.Inject

/**
 * Main extension class for configuring Telegram notifications in the build script.
 *
 * This extension provides a DSL for configuring all Telegram-related settings in your build script.
 * It allows you to define multiple bot configurations, changelog notification settings, and
 * distribution notification settings in a type-safe way.
 *
 * @see TelegramBotsConfig For bot configuration options
 * @see TelegramChangelogConfig For changelog configuration options
 * @see TelegramDistributionConfig For distribution configuration options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishTelegramExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for bot configurations, keyed by build type.
         *
         * This internal property holds all the bot configurations for different build types.
         * Use the [bots] and [botCommon] methods to configure these settings in your build script.
         */
        internal val bots: NamedDomainObjectContainer<TelegramBotsConfig> =
            objectFactory.domainObjectContainer(TelegramBotsConfig::class.java)

        /**
         * Container for changelog configurations, keyed by build type.
         *
         * This internal property holds all the changelog configurations for different build types.
         * Use the [changelog] and [changelogCommon] methods to configure these settings in your build script.
         */
        internal val changelog: NamedDomainObjectContainer<TelegramChangelogConfig> =
            objectFactory.domainObjectContainer(TelegramChangelogConfig::class.java)

        /**
         * Container for lookup configurations, keyed by build type.
         *
         * This internal property holds all the lookup configurations for different build types.
         * Use the [lookup] and [lookupCommon] methods to configure these settings in your build script.
         */
        internal val lookup: NamedDomainObjectContainer<TelegramLookupConfig> =
            objectFactory.domainObjectContainer(TelegramLookupConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<TelegramDistributionConfig> =
            objectFactory.domainObjectContainer(TelegramDistributionConfig::class.java)

        /**
         * Retrieves a required bot configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val botsConfig: (buildName: String) -> TelegramBotsConfig = { buildName ->
            bots.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional bot configuration for the specified build variant.
         * @return The bot configuration or null if not found
         */
        val botsConfigOrNull: (buildName: String) -> TelegramBotsConfig? = { buildName ->
            bots.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves a required changelog configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val changelogConfig: (buildName: String) -> TelegramChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional changelog configuration for the specified build variant.
         * @return The changelog configuration or null if not found
         */
        val changelogConfigOrNull: (buildName: String) -> TelegramChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves a required lookup configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val lookupConfig: (buildName: String) -> TelegramLookupConfig = { buildName ->
            lookup.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional lookup configuration for the specified build variant.
         *
         * @param buildName name of the build variant
         * @return The lookup configuration or null if not found
         */
        val lookupConfigOrNull: (buildName: String) -> TelegramLookupConfig? = { buildName ->
            lookup.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves a required distribution configuration for the specified build variant.
         * @throws UnknownDomainObjectException if no configuration exists for the build variant
         */
        val distributionConfig: (buildName: String) -> TelegramDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves an optional distribution configuration for the specified build variant.
         * @return The distribution configuration or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> TelegramDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures one or more Telegram bots for sending notifications.
         *
         * This method provides a DSL for defining bot configurations. Each bot must have a unique name
         * and can be configured with its own credentials and chat destinations.
         *
         * @param configurationAction Action to configure bot settings
         */
        fun bots(configurationAction: Action<BuildPublishDomainObjectContainer<TelegramBotsConfig>>) {
            val container = BuildPublishDomainObjectContainer(bots)
            configurationAction.execute(container)
        }

        /**
         * Configures changelog notification settings.
         *
         * This method provides a DSL for defining how changelog notifications should be sent to Telegram.
         * You can configure user mentions and specify which bots and chats should receive the notifications.
         *
         * @param configurationAction Action to configure changelog settings
         */
        fun changelog(configurationAction: Action<BuildPublishDomainObjectContainer<TelegramChangelogConfig>>) {
            val container = BuildPublishDomainObjectContainer(changelog)
            configurationAction.execute(container)
        }

        /**
         * Configures common Telegram lookup settings that apply to all build variants.
         *
         * This method allows you to configure Telegram lookup settings that apply to all
         * build variants. It registers a common configuration that applies to all
         * lookups using the [BuildPublishDomainObjectContainer] abstraction.
         *
         * @param configurationAction Action to configure common lookup settings
         */
        fun lookup(configurationAction: Action<TelegramLookupConfig>) {
            common(lookup, configurationAction)
        }

        /**
         * Configures distribution notification settings.
         *
         * This method provides a DSL for defining how distribution notifications (e.g., new app versions)
         * should be sent to Telegram. You can specify which bots and chats should receive these notifications.
         *
         * @param configurationAction Action to configure distribution settings
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<TelegramDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures common Telegram bot settings that apply to all build variants.
         *
         * This method allows you to configure Telegram bot settings that apply to all
         * build variants. It registers a common configuration that applies to all
         * bots using the [BuildPublishDomainObjectContainer] abstraction.
         *
         * @param configurationAction Action to configure common bot settings
         */
        fun botsCommon(configurationAction: Action<TelegramBotsConfig>) {
            common(bots, configurationAction)
        }

        /**
         * Configures common changelog settings that apply to all build variants.
         *
         * This method registers a common configuration that applies to all changelog settings
         * using the [BuildPublishDomainObjectContainer] abstraction. The configuration will
         * be applied to all changelog settings with variant-specific configurations as a
         * fallback.
         *
         * @param configurationAction Action to configure common changelog settings
         */
        fun changelogCommon(configurationAction: Action<TelegramChangelogConfig>) {
            common(changelog, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants.
         *
         * This method registers a common configuration that applies to all distribution settings
         * using the [BuildPublishDomainObjectContainer] abstraction. The configuration will
         * be applied to all distribution settings with variant-specific configurations as a
         * fallback.
         *
         * @param configurationAction Action to configure common distribution settings
         */
        fun distributionCommon(configurationAction: Action<TelegramDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures the Telegram plugin for the given project and build variant.
         *
         * This internal method is called during the project configuration phase to set up
         * the necessary tasks and services for Telegram notifications. It registers the
         * appropriate tasks based on the build variant and configuration.
         *
         * @param project The target project to configure
         * @param input Extension input containing build variant and configuration details
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val buildVariant = input.buildVariant.name

            if (bots.isEmpty()) {
                throw GradleException(needToProvideBotsConfigMessage(buildVariant))
            }
            val changelogConfig = changelogConfigOrNull(buildVariant)
            val distributionConfig = distributionConfigOrNull(buildVariant)
            val lookupConfig = lookupConfigOrNull(buildVariant)

            if (lookupConfig != null) {
                TelegramTasksRegistrar.registerLookupTask(
                    project,
                    lookupConfig,
                    TelegramLookupTaskParams(input.buildVariant)
                )
            }

            if (changelogConfig == null && distributionConfig == null && lookupConfig == null) {
                throw GradleException(
                    needToProvideChangelogOrDistributionConfigMessage(buildVariant)
                )
            }

            if (changelogConfig != null) {
                TelegramTasksRegistrar.registerChangelogTask(
                    project = project,
                    config = changelogConfig,
                    params =
                        TelegramChangelogTaskParams(
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
                TelegramTasksRegistrar.registerApkDistributionTask(
                    project = project,
                    config = distributionConfig,
                    params =
                        TelegramApkDistributionTaskParams(
                            baseFileName = input.output.baseFileName,
                            buildVariant = input.buildVariant,
                            lastBuildTag = input.output.lastBuildTagFile,
                            apkOutputFile = input.output.apkFile,
                        ),
                )

                TelegramTasksRegistrar.registerBundleDistributionTask(
                    project = project,
                    config = distributionConfig,
                    params =
                        TelegramBundleDistributionTaskParams(
                            baseFileName = input.output.baseFileName,
                            buildVariant = input.buildVariant,
                            lastBuildTag = input.output.lastBuildTagFile,
                            bundleOutputFile = input.output.bundleFile,
                        ),
                )
            }
        }
    }
