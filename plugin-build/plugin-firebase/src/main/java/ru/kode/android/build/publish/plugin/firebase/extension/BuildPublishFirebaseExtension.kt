package ru.kode.android.build.publish.plugin.firebase.extension

import com.android.build.gradle.AppExtension
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
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
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseDistributionConfig
import java.io.File
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

        override fun configure(project: Project, input: ExtensionInput) {
            val appExtension = project.extensions.findByType(AppExtension::class.java)
                ?: throw GradleException("AppExtension not found")
            val buildTypeName = input.buildVariant.buildTypeName

            val firebaseExtension = project.extensions.getByType(BuildPublishFirebaseExtension::class.java)
            val distributionConfig = firebaseExtension.distribution

            val config = distributionConfig.getByNameOrRequiredCommon(input.buildVariant.name)
            if (buildTypeName != null) {
                val buildType = appExtension.buildTypes.getByName(buildTypeName)
                buildType.firebaseAppDistribution {
                    val changelogFile: File? = input.output.changelogFileName.orNull?.asFile
                    this.applyConfig(config, changelogFile)
                }
            }
            input.buildVariant.productFlavors.forEach { flavor ->
                val flavor = appExtension.productFlavors.getByName(flavor.name)
                flavor.firebaseAppDistribution {
                    val changelogFile: File? = input.output.changelogFileName.orNull?.asFile
                    this.applyConfig(config, changelogFile)
                }
            }
        }
    }

/**
 * Applies Firebase App Distribution settings to a variant extension.
 */
private fun AppDistributionExtension.applyConfig(
    config: FirebaseDistributionConfig,
    changelogFile: File?,
) {
    config.appId.orNull
        ?.takeIf { it.isNotBlank() }
        ?.let { appId = it }

    config.serviceCredentialsFile.orNull?.asFile
        ?.takeIf { it.exists() }
        ?.let { serviceCredentialsFile = it.path }

    config.artifactType.orNull
        ?.let { artifactType = it }

    config.testerGroups.orNull
        ?.let { groups = it.joinToString(",") }

    changelogFile?.let { releaseNotesFile = it.path }
}
