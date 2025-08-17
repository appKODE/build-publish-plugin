package ru.kode.android.build.publish.plugin.appcenter.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterAuthConfig
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

/**
 * Main extension point for configuring AppCenter integration in your Gradle build script.
 *
 * This extension manages the configuration for publishing Android builds to AppCenter,
 * handling both authentication and distribution settings. It provides a flexible way to configure
 * different settings for different build variants while supporting common configurations.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for authentication configurations, keyed by build type.
         *
         * This internal property holds all the authentication configurations for different build types.
         * Use the [auth] and [authCommon] methods to configure these settings in your build script.
         */
        internal val auth: NamedDomainObjectContainer<AppCenterAuthConfig> =
            objectFactory.domainObjectContainer(AppCenterAuthConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)

        /**
         * Retrieves the authentication configuration for a specific build type.
         *
         * @param buildName The name of the build type (e.g., "debug", "release")
         *
         * @return The authentication configuration for the specified build type
         * @throws UnknownDomainObjectException If no configuration is found for the build type
         */
        val authConfig: (buildName: String) -> AppCenterAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the authentication configuration for a specific build type, if it exists.
         *
         * @param buildName The name of the build type (e.g., "debug", "release")
         *
         * @return The authentication configuration for the specified build type, or null if not found
         */
        val authConfigOrNull: (buildName: String) -> AppCenterAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves the distribution configuration for a specific build type.
         *
         * @param buildName The name of the build type (e.g., "debug", "release")
         *
         * @return The distribution configuration for the specified build type
         * @throws UnknownDomainObjectException If no configuration is found for the build type
         */
        val distributionConfig: (buildName: String) -> AppCenterDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the distribution configuration for a specific build type, if it exists.
         *
         * @param buildName The name of the build type (e.g., "debug", "release")
         *
         * @return The distribution configuration for the specified build type, or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> AppCenterDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for specific build types.
         *
         * Use this method to define different authentication settings for different build variants.
         * The configuration will be applied only to the specified build types, with common settings
         * from [authCommon] as a fallback.
         *
         * @param configurationAction The configuration block that sets up authentication for build types
         */
        fun auth(configurationAction: Action<BuildPublishDomainObjectContainer<AppCenterAuthConfig>>) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution settings for specific build types.
         *
         * Use this method to define different distribution settings for different build variants.
         * The configuration will be applied only to the specified build types, with common settings
         * from [distributionCommon] as a fallback.
         *
         * @param configurationAction The configuration block that sets up distribution for build types
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<AppCenterDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures authentication settings applied to all build types.
         *
         * Settings defined here act as fallbacks for all build types. They can be overridden
         * by build-type specific configurations in the [auth] block.
         *
         * @param configurationAction The configuration block that sets up common authentication settings
         */
        fun authCommon(configurationAction: Action<AppCenterAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures distribution settings applied to all build types.
         *
         * Settings defined here act as fallbacks for all build types. They can be overridden
         * by build-type specific configurations in the [distribution] block.
         *
         * @param configurationAction The configuration block that sets up common distribution settings
         */
        fun distributionCommon(configurationAction: Action<AppCenterDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures the AppCenter publishing tasks for the given project and build variant.
         *
         * This internal method is called by the plugin infrastructure to set up the necessary
         * tasks for publishing to AppCenter. It's not intended to be called directly from build scripts.
         *
         * @param project The Gradle project being configured
         * @param input The extension input containing build variant and file information
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val appCenterDistributionConfig = distributionConfig(input.buildVariant.name)

            AppCenterTasksRegistrar.registerDistributionTask(
                project = project,
                distributionConfig = appCenterDistributionConfig,
                params =
                    AppCenterDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        changelogFile = input.changelog.file,
                        apkOutputFile = input.output.apkFile,
                        lastBuildTagFile = input.output.lastBuildTagFile,
                        baseFileName = input.output.baseFileName,
                    ),
            )
        }
    }
