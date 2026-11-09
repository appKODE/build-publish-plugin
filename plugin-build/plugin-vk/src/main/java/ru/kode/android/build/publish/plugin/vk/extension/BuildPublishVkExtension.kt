package ru.kode.android.build.publish.plugin.vk.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.entity.ExtensionInput
import ru.kode.android.build.publish.plugin.vk.config.VkAuthConfig
import ru.kode.android.build.publish.plugin.vk.config.VkDistributionConfig
import ru.kode.android.build.publish.plugin.vk.messages.needProvideAuthConfigMessage
import ru.kode.android.build.publish.plugin.vk.messages.needProvideDistributionConfigMessage
import ru.kode.android.build.publish.plugin.vk.task.VkApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.vk.task.VkBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.vk.task.VkTasksRegistrar
import ru.kode.android.gradle.commons.api.container.PluginDomainObjectContainer
import ru.kode.android.gradle.commons.util.configureGroovy
import ru.kode.android.gradle.commons.util.getByNameOrNullableCommon
import ru.kode.android.gradle.commons.util.getByNameOrRequiredCommon
import javax.inject.Inject

/**
 * Extension for configuring VK publishing in the build and publish plugin.
 *
 * This extension allows configuration of VK authentication and distribution settings
 * for different build variants.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishVkExtension
    @Inject
    constructor(
        objectFactory: ObjectFactory,
    ) : BuildPublishConfigurableExtension() {
        /**
         * Container for VK authentication configurations.
         *
         * This container holds named configurations for authenticating with the VK API.
         * Each configuration is typically associated with a build variant or environment.
         */
        internal val auth: NamedDomainObjectContainer<VkAuthConfig> =
            objectFactory.domainObjectContainer(VkAuthConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<VkDistributionConfig> =
            objectFactory.domainObjectContainer(VkDistributionConfig::class.java)

        /**
         * Gets the authentication configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val authConfig: (buildName: String) -> VkAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the authentication configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration or null if not found
         */
        val authConfigOrNull: (buildName: String) -> VkAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val distributionConfig: (buildName: String) -> VkDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> VkDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for different build variants.
         *
         * @param configurationAction The action to configure authentication settings
         */
        fun auth(
            @DelegatesTo(PluginDomainObjectContainer::class)
            configurationAction: Action<in PluginDomainObjectContainer<VkAuthConfig>>,
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
            configurationClosure: Closure<in PluginDomainObjectContainer<VkAuthConfig>>,
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
            configurationAction: Action<in PluginDomainObjectContainer<VkDistributionConfig>>,
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
            configurationClosure: Closure<in PluginDomainObjectContainer<VkDistributionConfig>>,
        ) {
            val container = PluginDomainObjectContainer(distribution)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common authentication settings
         */
        fun authCommon(configurationAction: Action<in VkAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common authentication settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common authentication settings
         */
        fun authCommon(
            @DelegatesTo(
                value = VkAuthConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in VkAuthConfig>,
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
        fun distributionCommon(configurationAction: Action<in VkDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common distribution settings
         */
        fun distributionCommon(
            @DelegatesTo(
                value = VkDistributionConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in VkDistributionConfig>,
        ) {
            common(distribution) { target ->
                configureGroovy(configurationClosure, target)
            }
        }

        /**
         * Configures the VK publishing tasks for the current project.
         *
         * This method is called during the configuration phase to set up the necessary
         * tasks for publishing to VK.
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

            VkTasksRegistrar.registerApkDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    VkApkDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        apkOutputFile = input.output.apkFile,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        baseFileName = input.output.baseFileName,
                        buildVariantDefaultVersionName = input.buildVariant.defaultVersionName,
                    ),
            )

            VkTasksRegistrar.registerBundleDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    VkBundleDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        bundleOutputFile = input.output.bundleFile,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        baseFileName = input.output.baseFileName,
                        buildVariantDefaultVersionName = input.buildVariant.defaultVersionName,
                    ),
            )
        }
    }
