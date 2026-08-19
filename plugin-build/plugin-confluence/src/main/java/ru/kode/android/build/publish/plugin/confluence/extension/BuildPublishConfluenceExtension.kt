package ru.kode.android.build.publish.plugin.confluence.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.messages.needProvideAuthConfigMessage
import ru.kode.android.build.publish.plugin.confluence.messages.needProvideDistributionConfigMessage
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.entity.ExtensionInput
import ru.kode.android.gradle.commons.api.config.BasicAuthConfig
import ru.kode.android.gradle.commons.api.container.PluginDomainObjectContainer
import ru.kode.android.gradle.commons.util.configureGroovy
import ru.kode.android.gradle.commons.util.getByNameOrNullableCommon
import ru.kode.android.gradle.commons.util.getByNameOrRequiredCommon
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
        internal val auth: NamedDomainObjectContainer<BasicAuthConfig> =
            objectFactory.domainObjectContainer(BasicAuthConfig::class.java)

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
        val authConfig: (buildName: String) -> BasicAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the authentication configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration or null if not found
         */
        val authConfigOrNull: (buildName: String) -> BasicAuthConfig? = { buildName ->
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
        fun auth(
            @DelegatesTo(PluginDomainObjectContainer::class)
            configurationAction: Action<in PluginDomainObjectContainer<BasicAuthConfig>>,
        ) {
            val container = PluginDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures authentication settings for different build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure authentication settings
         */
        fun auth(
            @DelegatesTo(PluginDomainObjectContainer::class)
            configurationClosure: Closure<in PluginDomainObjectContainer<BasicAuthConfig>>,
        ) {
            val container = PluginDomainObjectContainer(auth)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures distribution settings for different build variants.
         *
         * @param configurationAction The action to configure distribution settings
         */
        fun distribution(
            @DelegatesTo(PluginDomainObjectContainer::class)
            configurationAction: Action<in PluginDomainObjectContainer<ConfluenceDistributionConfig>>,
        ) {
            val container = PluginDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution settings for different build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure distribution settings
         */
        fun distribution(
            @DelegatesTo(PluginDomainObjectContainer::class)
            configurationClosure: Closure<in PluginDomainObjectContainer<ConfluenceDistributionConfig>>,
        ) {
            val container = PluginDomainObjectContainer(distribution)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common authentication settings
         */
        fun authCommon(configurationAction: Action<in BasicAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common authentication settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common authentication settings
         */
        fun authCommon(
            @DelegatesTo(
                value = BasicAuthConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in BasicAuthConfig>,
        ) {
            common(auth) { target ->
                configureGroovy(configurationClosure, target)
            }
        }

        /**
         * Configures common distribution settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common distribution settings
         */
        fun distributionCommon(configurationAction: Action<in ConfluenceDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common distribution settings
         */
        fun distributionCommon(
            @DelegatesTo(
                value = ConfluenceDistributionConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in ConfluenceDistributionConfig>,
        ) {
            common(distribution) { target ->
                configureGroovy(configurationClosure, target)
            }
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
            variant: ApplicationVariant,
        ) {
            val variantName = input.buildVariant.name

            if (auth.isEmpty()) {
                throw GradleException(needProvideAuthConfigMessage(variantName))
            }

            val distributionConfig =
                distributionConfigOrNull(input.buildVariant.name)
                    ?: throw GradleException(needProvideDistributionConfigMessage(variantName))

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
