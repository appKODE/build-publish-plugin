package ru.kode.android.build.publish.plugin.core.api.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.buildVariant
import ru.kode.android.build.publish.plugin.core.util.common

/**
 * Base class for configurable extensions in the build and publish plugin system.
 *
 * This class provides common functionality for managing build configurations
 * across different build types and variants. It's designed to be extended by
 * specific extension implementations that need to handle configuration for
 * different build types (e.g., debug, release).
 *
 * @see BaseDomainContainer
 * @see NamedDomainObjectContainer
 */
open class BuildPublishConfigurableExtension {
    /**
     * Applies common configuration to a named domain object container.
     *
     * This method registers a common configuration that applies to all build types.
     *
     * @param T The type of objects in the container
     * @param container The container to configure
     * @param configurationAction The action that configures the common settings
     */
    protected fun <T> common(
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    /**
     * Applies common configuration to a base domain container.
     *
     * This method registers a common configuration that applies to all build types
     * using the [BuildPublishDomainObjectContainer] abstraction.
     *
     * @param T The type of objects in the container
     * @param container The base domain container to configure
     * @param configurationAction The action that configures the common settings
     */
    protected fun <T> common(
        container: BuildPublishDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    /**
     * Applies build-type specific configuration to a named domain object container.
     *
     * @param T The type of objects in the container
     * @param buildVariant The name of the variant type (e.g., "debug", "release")
     * @param container The container to configure
     * @param configurationAction The action that configures the build type
     */
    protected fun <T> buildVariant(
        buildVariant: String,
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Applies build-type specific configuration to a base domain container.
     *
     * @param T The type of objects in the container
     * @param buildVariant The name of the build variant (e.g., "debug", "release")
     * @param container The base domain container to configure
     * @param configurationAction The action that configures the build type
     */
    protected fun <T> buildVariant(
        buildVariant: String,
        container: BuildPublishDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Configures the extension with the given project and input.
     *
     * This method is called during the configuration phase to set up the extension.
     * Subclasses should override this method to provide their specific configuration logic.
     *
     * @param project The Gradle project being configured
     * @param input The extension input containing configuration data
     */
    open fun configure(
        project: Project,
        input: ExtensionInput,
    ) = Unit
}
