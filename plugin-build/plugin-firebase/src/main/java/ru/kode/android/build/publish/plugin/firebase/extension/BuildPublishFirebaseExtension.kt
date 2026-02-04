package ru.kode.android.build.publish.plugin.firebase.extension

import com.android.build.api.variant.ApplicationVariant
import com.google.firebase.appdistribution.gradle.AppDistributionVariantExtension
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
import ru.kode.android.build.publish.plugin.core.util.APK_FILE_EXTENSION
import ru.kode.android.build.publish.plugin.core.util.BUNDLE_FILE_EXTENSION
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseDistributionConfig
import ru.kode.android.build.publish.plugin.firebase.messages.appDistributionConfigNotFoundMessage
import ru.kode.android.build.publish.plugin.firebase.messages.provideDistributionConfigMessage
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
         * Container for Firebase App Distribution configurations.
         *
         * Configs are selected by build variant name (with support for a `common` config).
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
        @JvmSynthetic
        fun distribution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<BuildPublishDomainObjectContainer<FirebaseDistributionConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configurationAction.execute(container)
        }

        fun distribution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<FirebaseDistributionConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(distribution)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Configures common Firebase Distribution settings that apply to all build variants.
         *
         * @param configurationAction The action to configure the common distribution settings
         */
        @JvmSynthetic
        fun distributionCommon(configurationAction: Action<FirebaseDistributionConfig>) {
            common(distribution, configurationAction)
        }

        fun distributionCommon(
            @DelegatesTo(
                value = FirebaseDistributionConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in FirebaseDistributionConfig>
        ) {
            common(distribution) { target ->
                configureGroovy(configurationClosure, target)
            }
        }

        /**
         * Configures the official Firebase App Distribution variant extension.
         *
         * This method is called for each Android variant and sets:
         * - `appId`, `serviceCredentialsFile`, and `artifactType` from [FirebaseDistributionConfig]
         * - `groups` from configured tester groups
         * - `releaseNotesFile` from the build-publish changelog output
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
            variant: ApplicationVariant,
        ) {
            val buildVariantName = input.buildVariant.name
            val distributionConfig = distributionConfigOrNull(buildVariantName)

            if (distributionConfig == null) {
                throw GradleException(provideDistributionConfigMessage(buildVariantName))
            }

            variant.getExtension(AppDistributionVariantExtension::class.java)?.apply {
                appId.set(distributionConfig.appId)
                serviceCredentialsFile.set(distributionConfig.serviceCredentialsFile.map { it.asFile.path })
                artifactType.set(
                    distributionConfig.artifactType.map {
                        when (it) {
                            ArtifactType.Apk -> APK_FILE_EXTENSION.uppercase()
                            ArtifactType.Bundle -> BUNDLE_FILE_EXTENSION.uppercase()
                        }
                    },
                )
                groups.set(distributionConfig.testerGroups.map { it.joinToString(",") })
                releaseNotesFile.set(input.changelog.fileProvider.map { it.changelogFile.get().asFile.path })
            } ?: throw GradleException(appDistributionConfigNotFoundMessage(buildVariantName))
        }
    }
