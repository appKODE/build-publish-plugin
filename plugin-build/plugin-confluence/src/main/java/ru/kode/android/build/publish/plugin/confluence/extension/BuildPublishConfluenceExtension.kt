package ru.kode.android.build.publish.plugin.confluence.extension

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceAuthConfig
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

/**
 * Extension for configuring Confluence publishing in the build and publish plugin.
 *
 * This extension allows configuration of Confluence authentication and distribution settings
 * for different build variants.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishConfluenceExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for Confluence authentication configurations.
         *
         * This container holds named configurations for authenticating with the Confluence API.
         * Each configuration is typically associated with a build variant or environment.
         */
        internal val auth: NamedDomainObjectContainer<ConfluenceAuthConfig> =
            objectFactory.domainObjectContainer(ConfluenceAuthConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<ConfluenceDistributionConfig> =
            objectFactory.domainObjectContainer(ConfluenceDistributionConfig::class.java)

        /**
         * Gets the authentication configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val authConfig: (buildName: String) -> ConfluenceAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the authentication configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration or null if not found
         */
        val authConfigOrNull: (buildName: String) -> ConfluenceAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val distributionConfig: (buildName: String) -> ConfluenceDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> ConfluenceDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for different build variants.
         *
         * @param configurationAction The action to configure authentication settings
         */
        fun auth(configurationAction: Action<BuildPublishDomainObjectContainer<ConfluenceAuthConfig>>) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution settings for different build variants.
         *
         * @param configurationAction The action to configure distribution settings
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<ConfluenceDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common authentication settings
         */
        fun authCommon(configurationAction: Action<ConfluenceAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common distribution settings
         */
        fun distributionCommon(configurationAction: Action<ConfluenceDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures the Confluence publishing tasks for the current project.
         *
         * This method is called during the configuration phase to set up the necessary
         * tasks for publishing to Confluence.
         *
         * @param project The Gradle project being configured
         * @param input The extension input containing build and version information
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {

            val variantName = input.buildVariant.name

            if (auth.isEmpty()) {
                throw GradleException(
                    "Need to provide Auth config for `$variantName` or `common`. " +
                        "It's required to run Jira plugin. " +
                        "Please check that you have 'auth' block in your build script " +
                        "and that it's not empty. "
                )
            }

            val distributionConfig = distributionConfigOrNull(input.buildVariant.name)
                ?: throw GradleException(
                    "Need to provide Distribution config for `$variantName` or `common`. " +
                        "Please check that you have 'distribution' block in your build script " +
                        "and that it's not empty. "
                )

            ConfluenceTasksRegistrar.registerApkDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    ConfluenceApkDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        apkOutputFile = input.output.apkFile,
                    ),
            )

            ConfluenceTasksRegistrar.registerBundleDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    ConfluenceBundleDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        bundleOutputFile = input.output.bundleFile,
                    ),
            )
        }
    }
