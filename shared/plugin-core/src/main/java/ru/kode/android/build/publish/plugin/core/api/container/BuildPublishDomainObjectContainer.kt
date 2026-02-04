package ru.kode.android.build.publish.plugin.core.api.container

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.UnknownDomainObjectException
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.buildVariant
import ru.kode.android.build.publish.plugin.core.util.common

/**
 * A specialized container for managing build configuration objects with support for common and variant-specific settings.
 *
 * This class extends the functionality of Gradle's [NamedDomainObjectContainer] by adding support for:
 * - **Common Configurations**: Settings that apply to all build variants
 * - **Variant-Specific Overrides**: Settings that apply only to specific build variants
 * - **Type-Safe Access**: Compile-time type checking for configuration objects
 * - **Lazy Configuration**: Deferred application of configuration actions
 *
 * The container is designed to work with the [BuildPublishConfigurableExtension] to provide a fluent,
 * type-safe DSL for build configuration. It's particularly useful in Android projects where different
 * build types and product flavors require different configurations.
 *
 * @param T The type of configuration objects contained in this container. Must have a name property.
 * @property namedContainer The underlying Gradle [NamedDomainObjectContainer] that stores the configurations.
 *
 * @see NamedDomainObjectContainer For the base container implementation
 * @see BuildPublishConfigurableExtension For the extension that typically uses this container
 *
 * @sample
 * ```kotlin
 * // Create a container for your configuration type
 * val container = BuildPublishDomainObjectContainer<MyConfig>(
 *     project.container(MyConfig::class.java) { name ->
 *         objects.newInstance(MyConfig::class.java, name)
 *     }
 * )
 *
 * // Apply common configuration
 * container.common {
 *     commonProperty = "value-for-all-variants"
 * }
 *
 * // Apply variant-specific configuration
 * container.buildVariant("debug") {
 *     debugOnlyProperty = true
 * }
 * ```
 */
class BuildPublishDomainObjectContainer<T : Any>(
    private val namedContainer: NamedDomainObjectContainer<T>,
) {
    /**
     * Registers a common configuration that applies to all build types and variants.
     *
     * The common configuration serves as a base that can be overridden by variant-specific
     * configurations. When a configuration is requested for a specific variant, the common
     * configuration is applied first, followed by any variant-specific configurations.
     *
     * @param configurationAction The action that configures the common settings. This action
     *                          will be applied to all configurations created by this container.
     * @return A [NamedDomainObjectProvider] that can be used to access the configuration.
     *
     * @see buildVariant For defining variant-specific configurations
     *
     * @sample
     * ```kotlin
     * container.common {
     *     // These settings apply to all variants
     *     enableFeatureX = true
     *     logLevel = LogLevel.INFO
     *
     *     // Can be overridden by variant-specific configurations
     *     apiEndpoint = "https://api.example.com/production"
     * }
     * ```
     */
    @JvmSynthetic
    fun common(
        @DelegatesTo.Target
        configurationAction: Action<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.common(configurationAction)
    }

    fun common(
        @DelegatesTo.Target
        configurationClosure: Closure<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.common(
            Action { target ->
                configureGroovy(configurationClosure, target)
            },
        )
    }

    /**
     * Registers a configuration specific to a build variant.
     *
     * The configuration action will only be applied when the named variant is being built.
     * Variant-specific configurations are merged with the common configuration, with the
     * variant settings taking precedence in case of conflicts.
     *
     * @param buildVariant The name of the build variant (e.g., "debug", "release", "demoDebug").
     *                    This should match the variant name used in the build system.
     * @param configurationAction The action that configures the variant-specific settings.
     * @return A [NamedDomainObjectProvider] that can be used to access the configuration.
     *
     * @see common For defining configurations that apply to all variants
     *
     * @sample
     * ```kotlin
     * // Configure debug variant
     * container.buildVariant("debug") {
     *     enableDebugFeatures = true
     *     logLevel = LogLevel.VERBOSE
     *     apiEndpoint = "https://api.example.com/staging"
     * }
     *
     * // Configure release variant
     * container.buildVariant("release") {
     *     enableProguard = true
     *     logLevel = LogLevel.WARNING
     *     enableAnalytics = true
     * }
     * ```
     */
    @JvmSynthetic
    fun buildVariant(
        buildVariant: String,
        @DelegatesTo.Target
        configurationAction: Action<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.buildVariant(buildVariant, configurationAction)
    }

    fun buildVariant(
        buildVariant: String,
        @DelegatesTo.Target
        configurationClosure: Closure<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.buildVariant(
            buildVariant,
            Action { target ->
                configureGroovy(configurationClosure, target)
            },
        )
    }

    /**
     * Finds a configuration by name, returning null if not found.
     *
     * This is a safe alternative to [getByName] that returns null instead of throwing an exception
     * when the configuration doesn't exist. Use this when you're not sure if the configuration exists.
     *
     * @param name The name of the configuration to find. This is typically the variant name.
     * @return The configuration with the given name, or `null` if no such configuration exists.
     *
     * @see getByName For a version that throws an exception when the configuration doesn't exist
     *
     * @sample
     * ```kotlin
     * // Safe lookup that won't throw an exception
     * val config = container.findByName(variantName)
     * if (config != null) {
     *     // Use the configuration
     *     applyConfiguration(config)
     * } else {
     *     // Handle missing configuration
     *     logger.warn("No configuration found for variant: $variantName")
     * }
     * ```
     */
    fun findByName(name: String): T? {
        return namedContainer.findByName(name)
    }

    /**
     * Gets a configuration by name, throwing an exception if not found.
     *
     * This method provides direct access to a configuration and is appropriate when you expect
     * the configuration to exist. If the configuration might not exist, use [findByName] instead.
     *
     * @param name The name of the configuration to retrieve. This is typically the variant name.
     * @return The configuration with the given name.
     * @throws UnknownDomainObjectException If no configuration with the given name exists.
     *
     * @see findByName For a null-safe alternative that returns null for missing configurations
     *
     * @sample
     * ```kotlin
     * try {
     *     // Will throw if the configuration doesn't exist
     *     val config = container.getByName(variantName)
     *     applyConfiguration(config)
     * } catch (e: UnknownDomainObjectException) {
     *     // Handle missing configuration
     *     throw GradleException("Required configuration not found for variant: $variantName")
     * }
     * ```
     */
    @Throws(UnknownDomainObjectException::class)
    fun getByName(name: String): T {
        return namedContainer.getByName(name)
    }
}
