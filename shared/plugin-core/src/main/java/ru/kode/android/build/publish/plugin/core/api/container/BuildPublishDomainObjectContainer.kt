package ru.kode.android.build.publish.plugin.core.api.container

import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.UnknownDomainObjectException
import ru.kode.android.build.publish.plugin.core.util.buildVariant
import ru.kode.android.build.publish.plugin.core.util.common

/**
 * A wrapper class for [NamedDomainObjectContainer] that provides additional functionality
 * for managing domain objects with common configurations.
 *
 * This class simplifies the management of build configurations by providing methods to:
 * - Register common configurations that apply to all build types
 * - Register build-type specific configurations
 * - Look up configurations by name
 *
 * @param T The type of objects contained in this container
 * @property namedContainer The underlying Gradle [NamedDomainObjectContainer]
 *
 * @see NamedDomainObjectContainer
 */
class BuildPublishDomainObjectContainer<T : Any>(
    private val namedContainer: NamedDomainObjectContainer<T>,
) {
    /**
     * Registers a common configuration that applies to all build types.
     *
     * The common configuration will be merged with build-type specific configurations,
     * with build-type configurations taking precedence.
     *
     * @param configurationAction The action to configure the common settings
     * @return A provider for the registered configuration
     */
    fun common(
        @DelegatesTo.Target
        configurationAction: Action<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.common(configurationAction)
    }

    /**
     * Registers a build-type specific configuration.
     *
     * @param buildVariant The name of the build variant (e.g., "debug", "release", "googleDebug")
     * @param configurationAction The action to configure the build type
     * @return A provider for the registered build type configuration
     *
     * @see common
     */
    fun buildVariant(
        buildVariant: String,
        @DelegatesTo.Target
        configurationAction: Action<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Finds a configuration by name, returning null if not found.
     *
     * @param name The name of the configuration to find
     * @return The configuration, or null if no configuration with the given name exists
     *
     * @see getByName
     */
    fun findByName(name: String): T? {
        return namedContainer.findByName(name)
    }

    /**
     * Gets a configuration by name, throwing an exception if not found.
     *
     * @param name The name of the configuration to get
     * @return The configuration with the given name
     * @throws UnknownDomainObjectException If no configuration with the given name exists
     *
     * @see findByName
     */
    @Throws(UnknownDomainObjectException::class)
    fun getByName(name: String): T {
        return namedContainer.getByName(name)
    }
}
