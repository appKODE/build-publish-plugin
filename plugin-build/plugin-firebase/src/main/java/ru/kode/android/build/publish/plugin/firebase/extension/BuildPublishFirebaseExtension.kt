package ru.kode.android.build.publish.plugin.firebase.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseDistributionConfig
import javax.inject.Inject

/**
 * Extension class for configuring Firebase App Distribution in the build and publish plugin.
 *
 * This extension provides configuration options for distributing Android applications
 * through Firebase App Distribution. It allows defining different distribution settings
 * for various build variants and provides convenient accessors for these configurations.
 *
 * @see BuildPublishConfigurableExtension
 * @see FirebaseDistributionConfig
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFirebaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for distribution configurations, keyed by build type.
         *
         * This internal property holds all the distribution configurations for different build types.
         * Use the [distribution] and [distributionCommon] methods to configure these settings in your build script.
         */
        val distribution: NamedDomainObjectContainer<FirebaseDistributionConfig> =
            objectFactory.domainObjectContainer(FirebaseDistributionConfig::class.java)

        /**
         * Retrieves the Firebase Distribution configuration for the specified build variant.
         *
         * @param buildName The name of the build variant
         * @return The [FirebaseDistributionConfig] for the specified build
         * @throws UnknownDomainObjectException If no configuration exists for the build variant
         */
        val distributionConfig: (buildName: String) -> FirebaseDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the Firebase Distribution configuration for the specified build variant, or null if not found.
         *
         * @param buildName The name of the build variant
         * @return The [FirebaseDistributionConfig] for the specified build, or null if not found
         */
        val distributionConfigOrNull: (buildName: String) -> FirebaseDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures Firebase Distribution settings for different build variants.
         *
         * @param configurationAction The action to configure the distribution container
         * @see FirebaseDistributionConfig
         */
        fun distribution(configurationAction: Action<BuildPublishDomainObjectContainer<FirebaseDistributionConfig>>) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures common Firebase Distribution settings that apply to all build variants.
         *
         * @param configurationAction The action to configure the common distribution settings
         */
        fun distributionCommon(configurationAction: Action<FirebaseDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
