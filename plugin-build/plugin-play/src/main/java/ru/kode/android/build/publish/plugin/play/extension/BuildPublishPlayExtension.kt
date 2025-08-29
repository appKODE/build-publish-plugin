package ru.kode.android.build.publish.plugin.play.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.play.config.PlayAuthConfig
import ru.kode.android.build.publish.plugin.play.config.PlayDistributionConfig
import ru.kode.android.build.publish.plugin.play.task.PlayTaskParams
import ru.kode.android.build.publish.plugin.play.task.PlayTasksRegistrar
import javax.inject.Inject

/**
 * Main extension class for Google Play publishing configuration in the build-publish plugin.
 *
 * This extension provides configuration options for publishing Android applications to the Google Play Store.
 * It allows configuring authentication and distribution settings for different build variants.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishPlayExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for Google Play authentication configurations.
         *
         * This container holds named configurations for authenticating with the Google Play API.
         * Each configuration is typically associated with a build variant or environment.
         */
        internal val auth: NamedDomainObjectContainer<PlayAuthConfig> =
            objectFactory.domainObjectContainer(PlayAuthConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<PlayDistributionConfig> =
            objectFactory.domainObjectContainer(PlayDistributionConfig::class.java)

        /**
         * Retrieves the authentication configuration for the specified build variant.
         *
         * @throws UnknownDomainObjectException if no configuration is found for the build variant
         * @param buildName The name of the build variant
         * @return The authentication configuration for the specified build variant
         */
        val authConfig: (buildName: String) -> PlayAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the authentication configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant
         * @return The authentication configuration for the specified build variant, or null if not found
         */
        val authConfigOrNull: (buildName: String) -> PlayAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves the distribution configuration for the specified build variant.
         *
         * @throws UnknownDomainObjectException if no configuration is found for the build variant
         * @param buildName The name of the build variant
         * @return The distribution configuration for the specified build variant
         */
        val distributionConfig: (buildName: String) -> PlayDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the distribution configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant
         * @return The distribution configuration for the specified build variant, or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> PlayDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for different build variants.
         *
         * @param configurationAction The action to configure authentication settings
         */
        fun auth(configurationAction: Action<BuildPublishDomainObjectContainer<PlayAuthConfig>>) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution settings for different build variants.
         *
         * @param configurationAction The action to configure distribution settings
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<PlayDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         * These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The action to configure common authentication settings
         */
        fun authCommon(configurationAction: Action<PlayAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants.
         * These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The action to configure common distribution settings
         */
        fun distributionCommon(configurationAction: Action<PlayDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures the Google Play publishing tasks for the given project and build variant.
         * This method is called by the build-publish plugin during the configuration phase.
         *
         * @param project The Gradle project being configured
         * @param input The extension input containing build variant and output information
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val distributionConfig = distributionConfig(input.buildVariant.name)

            PlayTasksRegistrar.registerDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    PlayTaskParams(
                        buildVariant = input.buildVariant,
                        bundleOutputFile = input.output.bundleFile,
                        lastBuildTagFile = input.output.lastBuildTagFile,
                    ),
            )
        }
    }
