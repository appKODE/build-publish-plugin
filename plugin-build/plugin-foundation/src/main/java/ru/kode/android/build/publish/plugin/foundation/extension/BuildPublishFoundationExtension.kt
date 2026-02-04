package ru.kode.android.build.publish.plugin.foundation.extension

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.foundation.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.config.OutputConfig
import javax.inject.Inject

/**
 * Extension class for configuring the build and publish foundation plugin.
 *
 * This extension provides configuration options for build outputs and changelog generation.
 * It allows defining named configurations for different build variants and provides
 * convenient accessors for retrieving these configurations.
 *
 * @see BuildPublishConfigurableExtension
 * @see OutputConfig
 * @see ChangelogConfig
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFoundationExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Enables verbose logging for the build and publish plugins.
         *
         * If set to `true`, the plugin will print more detailed logs during the build process.
         *
         * Default value is `false`.
         */
        val verboseLogging: Property<Boolean> =
            objectFactory.property(Boolean::class.java)
                .convention(false)

        /**
         * Enables body logging for the build and publish plugins.
         *
         * If set to `true`, the plugin will print the body of the HTTP request and response
         * during the build process.
         *
         * Default value is `false`.
         */
        val bodyLogging: Property<Boolean> =
            objectFactory.property(Boolean::class.java)
                .convention(false)

        /**
         * Container for output configurations.
         *
         * Configs are selected by build variant name (with support for a `common` config).
         * Use the [output] and [outputCommon] methods to configure these settings in your build script.
         */
        internal val output: NamedDomainObjectContainer<OutputConfig> =
            objectFactory.domainObjectContainer(OutputConfig::class.java)

        /**
         * Container for changelog configurations.
         *
         * Configs are selected by build variant name (with support for a `common` config).
         * Use the [changelog] and [changelogCommon] methods to configure these settings in your build script.
         */
        internal val changelog: NamedDomainObjectContainer<ChangelogConfig> =
            objectFactory.domainObjectContainer(ChangelogConfig::class.java)

        /**
         * Retrieves the output configuration for the specified build variant.
         *
         * @param buildName The name of the build variant
         * @return The [OutputConfig] for the specified build
         * @throws UnknownDomainObjectException If no configuration exists for the build variant
         */
        val outputConfig: (buildName: String) -> OutputConfig = { buildName ->
            output.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the output configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant
         * @return The [OutputConfig] for the specified build, or null if not found
         */
        val outputConfigOrNull: (buildName: String) -> OutputConfig? = { buildName ->
            output.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves the changelog configuration for the specified build variant.
         *
         * @param buildName The name of the build variant
         * @return The [ChangelogConfig] for the specified build
         * @throws UnknownDomainObjectException If no configuration exists for the build variant
         */
        val changelogConfig: (buildName: String) -> ChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the changelog configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant
         * @return The [ChangelogConfig] for the specified build, or null if not found
         */
        val changelogConfigOrNull: (buildName: String) -> ChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures output settings for different build variants.
         *
         * Use this to declare per-variant (and common) output settings such as tag pattern,
         * version strategies, and APK naming.
         *
         * @param configurationAction The action to configure the output container.
         * @see OutputConfig
         */
        fun output(
            configurationAction: Action<in BuildPublishDomainObjectContainer<OutputConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(output)
            configurationAction.execute(container)
        }

        fun output(
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<OutputConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(output)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures changelog settings for different build variants.
         *
         * Use this to declare per-variant (and common) changelog parsing settings such as
         * issue number extraction and commit message markers.
         *
         * @param configurationAction The action to configure the changelog container.
         * @see ChangelogConfig
         */
        @JvmSynthetic
        fun changelog(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<in BuildPublishDomainObjectContainer<ChangelogConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(changelog)
            configurationAction.execute(container)
        }

        fun changelog(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<ChangelogConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(changelog)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures common output settings that apply to all build variants.
         *
         * @param configurationAction The action to configure the common output settings
         */
        @JvmSynthetic
        fun outputCommon(configurationAction: Action<OutputConfig>) {
            common(output, configurationAction)
        }

        fun outputCommon(
            @DelegatesTo(
                value = OutputConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in OutputConfig>
        ) {
            common(output) { target ->
                configureGroovy(configurationClosure, target)
            }
        }

        /**
         * Configures common changelog settings that apply to all build variants.
         *
         * @param configurationAction The action to configure the common changelog settings
         */
        @JvmSynthetic
        fun changelogCommon(configurationAction: Action<in ChangelogConfig>) {
            common(changelog, configurationAction)
        }

        fun changelogCommon(
            @DelegatesTo(
                value = ChangelogConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in ChangelogConfig>
        ) {
            common(changelog) { target ->
                configureGroovy(configurationClosure, target)
            }
        }
    }
