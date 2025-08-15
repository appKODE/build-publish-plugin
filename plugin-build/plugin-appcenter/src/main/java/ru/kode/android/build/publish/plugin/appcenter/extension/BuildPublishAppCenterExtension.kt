package ru.kode.android.build.publish.plugin.appcenter.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterAuthConfig
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

/**
 * Extension entry point for configuring App Center integration in the build script.
 *
 * Holds and manages two main configuration groups:
 * - [AppCenterAuthConfig] for authentication
 * - [AppCenterDistributionConfig] for app distribution
 *
 * Provides:
 * - Per-build-type configuration (e.g., `auth {}`, `distribution {}`)
 * - Common (fallback) configuration shared across build types (`authCommon {}`, `distributionCommon {}`)
 * - Utility functions to retrieve configs by build name (required and optional variants)
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        // Stores authentication configs for different build types.
        internal val auth: NamedDomainObjectContainer<AppCenterAuthConfig> =
            objectFactory.domainObjectContainer(AppCenterAuthConfig::class.java)

        // Stores distribution configs for different build types.
        internal val distribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)

        // Retrieves auth config for a specific build type (throws if not found).
        val authConfig: (buildName: String) -> AppCenterAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        // Retrieves auth config for a specific build type, or null if not found.
        val authConfigOrNull: (buildName: String) -> AppCenterAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        // Retrieves distribution config for a specific build type (throws if not found).
        val distributionConfig: (buildName: String) -> AppCenterDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        // Retrieves distribution config for a specific build type, or null if not found.
        val distributionConfigOrNull: (buildName: String) -> AppCenterDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication for specific build types.
         */
        fun auth(configurationAction: Action<BaseDomainContainer<AppCenterAuthConfig>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures distribution for specific build types.
         */
        fun distribution(configurationAction: Action<BaseDomainContainer<AppCenterDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        /**
         * Configures authentication settings applied to all build types (fallback).
         */
        fun authCommon(configurationAction: Action<AppCenterAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures distribution settings applied to all build types (fallback).
         */
        fun distributionCommon(configurationAction: Action<AppCenterDistributionConfig>) {
            common(distribution, configurationAction)
        }

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
