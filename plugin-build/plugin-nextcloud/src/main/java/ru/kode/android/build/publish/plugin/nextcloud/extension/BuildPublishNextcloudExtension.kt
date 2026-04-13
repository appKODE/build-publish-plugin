package ru.kode.android.build.publish.plugin.nextcloud.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudAuthConfig
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudDistributionConfig
import ru.kode.android.build.publish.plugin.nextcloud.messages.needProvideAuthConfigMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.needProvideDistributionConfigMessage
import ru.kode.android.build.publish.plugin.nextcloud.task.NextcloudApkDistributionTaskParams
import ru.kode.android.build.publish.plugin.nextcloud.task.NextcloudBundleDistributionTaskParams
import ru.kode.android.build.publish.plugin.nextcloud.task.NextcloudChangelogTaskParams
import ru.kode.android.build.publish.plugin.nextcloud.task.NextcloudTasksRegistrar
import javax.inject.Inject

/**
 * Extension for configuring Nextcloud publishing in the build and publish plugin.
 *
 * This extension allows configuration of Nextcloud authentication and distribution settings
 * for different build variants.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishNextcloudExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for Nextcloud authentication configurations.
         *
         * This container holds named configurations for authenticating with the Nextcloud API.
         * Each configuration is typically associated with a build variant or environment.
         */
        internal val auth: NamedDomainObjectContainer<NextcloudAuthConfig> =
            objectFactory.domainObjectContainer(NextcloudAuthConfig::class.java)

        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        internal val distribution: NamedDomainObjectContainer<NextcloudDistributionConfig> =
            objectFactory.domainObjectContainer(NextcloudDistributionConfig::class.java)

        /**
         * Gets the authentication configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val authConfig: (buildName: String) -> NextcloudAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the authentication configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The authentication configuration or null if not found
         */
        val authConfigOrNull: (buildName: String) -> NextcloudAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration for the build variant
         * @throws UnknownDomainObjectException If no configuration is found for the build variant
         */
        val distributionConfig: (buildName: String) -> NextcloudDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Gets the distribution configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant (e.g., "debug", "release")
         * @return The distribution configuration or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> NextcloudDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for different build variants.
         *
         * @param configurationAction The action to configure authentication settings
         */
        fun auth(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<in BuildPublishDomainObjectContainer<NextcloudAuthConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures authentication settings for different build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure authentication settings
         */
        fun auth(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<NextcloudAuthConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(auth)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures distribution settings for different build variants.
         *
         * @param configurationAction The action to configure distribution settings
         */
        fun distribution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<in BuildPublishDomainObjectContainer<NextcloudDistributionConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution settings for different build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure distribution settings
         */
        fun distribution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<NextcloudDistributionConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         *
         * @param configurationAction The action to configure common authentication settings
         */
        fun authCommon(configurationAction: Action<in NextcloudAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common authentication settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common authentication settings
         */
        fun authCommon(
            @DelegatesTo(
                value = NextcloudAuthConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in NextcloudAuthConfig>,
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
        fun distributionCommon(configurationAction: Action<in NextcloudDistributionConfig>) {
            common(distribution, configurationAction)
        }

        /**
         * Configures common distribution settings that apply to all build variants using Groovy DSL.
         *
         * @param configurationClosure The Groovy closure to configure common distribution settings
         */
        fun distributionCommon(
            @DelegatesTo(
                value = NextcloudDistributionConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in NextcloudDistributionConfig>,
        ) {
            common(distribution) { target ->
                configureGroovy(configurationClosure, target)
            }
        }

        /**
         * Configures the Nextcloud publishing tasks for the current project.
         *
         * This method is called during the configuration phase to set up the necessary
         * tasks for publishing to Nextcloud.
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

            NextcloudTasksRegistrar.registerApkDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    NextcloudApkDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        apkOutputFile = input.output.apkFile,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        baseFileName = input.output.baseFileName,
                        buildVariantDefaultVersionName = input.buildVariant.defaultVersionName,
                    ),
            )

            NextcloudTasksRegistrar.registerBundleDistributionTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    NextcloudBundleDistributionTaskParams(
                        buildVariant = input.buildVariant,
                        bundleOutputFile = input.output.bundleFile,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        baseFileName = input.output.baseFileName,
                        buildVariantDefaultVersionName = input.buildVariant.defaultVersionName,
                    ),
            )

            NextcloudTasksRegistrar.registerChangelogTask(
                project = project,
                distributionConfig = distributionConfig,
                params =
                    NextcloudChangelogTaskParams(
                        buildVariant = input.buildVariant,
                        changelogFileProvider = input.changelog.fileProvider,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        baseFileName = input.output.baseFileName,
                        buildVariantDefaultVersionName = input.buildVariant.defaultVersionName,
                    ),
            )
        }
    }
