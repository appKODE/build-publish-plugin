package ru.kode.android.build.publish.plugin.core.api.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
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
 * This abstract class serves as the foundation for creating configurable extensions that can be used
 * to customize the build and publish process for different build variants. It provides a consistent
 * way to apply common configurations and build-type specific settings through a type-safe DSL.
 *
 * Key features:
 * - Common configuration that applies to all build variants
 * - Build-type specific configuration (e.g., debug, release)
 * - Support for both standard Gradle containers and custom domain object containers
 * - Type-safe configuration through generic type parameters
 *
 * Implementations should override the [configure] method to provide specific configuration logic
 * for different build variants.
 *
 * @see BuildPublishDomainObjectContainer For the custom container implementation used by this extension
 * @see NamedDomainObjectContainer For the standard Gradle container interface
 *
 * @sample
 * ```kotlin
 * class MyExtension : BuildPublishConfigurableExtension() {
 *     // Extension properties and methods
 * }
 * ```
 */
open class BuildPublishConfigurableExtension {
    /**
     * Applies common configuration to a named domain object container.
     *
     * This method registers a configuration action that will be applied to all build variants.
     * The configuration is stored and applied when the container is populated with actual elements.
     *
     * @param T The type of objects in the container.
     * @param container The named domain object container to configure.
     * @param configurationAction The action that configures the common settings.
     *
     * @sample
     * ```kotlin
     * val myContainer = project.container(MyType::class)
     * extension.common(myContainer) {
     *     // Common configuration for all build types
     *     someProperty = "common-value"
     * }
     * ```
     */
    protected fun <T : Any> common(
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    protected fun <T : Any> common(
        container: NamedDomainObjectContainer<T>,
        @DelegatesTo(
            genericTypeIndex = 0,
            strategy = Closure.DELEGATE_FIRST,
        )
        configurationClosure: Closure<in T>,
    ) {
        container.common(configurationClosure)
    }

    /**
     * Applies common configuration to a base domain container.
     *
     * This method registers a configuration action that will be applied to all build variants
     * using the [BuildPublishDomainObjectContainer] abstraction. It's particularly useful
     * when working with custom domain object containers that require special handling.
     *
     * @param T The type of objects in the container.
     * @param container The base domain container to configure.
     * @param configurationAction The action that configures the common settings.
     *
     * @sample
     * ```kotlin
     * val myContainer = BuildPublishDomainObjectContainer(MyType::class.java, project.objects)
     * extension.common(myContainer) {
     *     // Common configuration for all build types
     *     someProperty = "common-value"
     * }
     * ```
     */
    protected fun <T : Any> common(
        container: BuildPublishDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    protected fun <T : Any> common(
        container: BuildPublishDomainObjectContainer<T>,
        @DelegatesTo(
            genericTypeIndex = 0,
            strategy = Closure.DELEGATE_FIRST,
        )
        configurationClosure: Closure<in T>,
    ) {
        container.common(configurationClosure)
    }

    /**
     * Applies build-type specific configuration to a named domain object container.
     *
     * This method registers a configuration action that will be applied only to the specified
     * build variant. The configuration is stored and applied when the container is populated
     * with actual elements for the given variant.
     *
     * @param T The type of objects in the container.
     * @param buildVariant The name of the build variant (e.g., "debug", "release").
     * @param container The named domain object container to configure.
     * @param configurationAction The action that configures the build type.
     *
     * @sample
     * ```kotlin
     * val myContainer = project.container(MyType::class)
     * extension.buildVariant("debug", myContainer) {
     *     // Configuration specific to debug builds
     *     someProperty = "debug-value"
     * }
     * ```
     */
    protected fun <T : Any> buildVariant(
        buildVariant: String,
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Applies build-type specific configuration to a named domain object container using the Closure syntax.
     *
     * This method registers a configuration action that will be applied only to the specified
     * build variant. The configuration is stored and applied when the container is populated
     * with actual elements for the given variant.
     *
     * @param T The type of objects in the container.
     * @param buildVariant The name of the build variant (e.g., "debug", "release").
     * @param container The named domain object container to configure.
     * @param configurationClosure The closure that configures the build type.
     * ```
     */
    protected fun <T : Any> buildVariant(
        buildVariant: String,
        container: NamedDomainObjectContainer<T>,
        @DelegatesTo(
            genericTypeIndex = 0,
            strategy = Closure.DELEGATE_FIRST,
        )
        configurationClosure: Closure<in T>,
    ) {
        container.buildVariant(buildVariant, configurationClosure)
    }

    /**
     * Applies build-type specific configuration to a base domain container.
     *
     * This method registers a configuration action that will be applied only to the specified
     * build variant using the [BuildPublishDomainObjectContainer] abstraction. It's particularly
     * useful when working with custom domain object containers that require special handling.
     *
     * @param T The type of objects in the container.
     * @param buildVariant The name of the build variant (e.g., "debug", "release").
     * @param container The base domain container to configure.
     * @param configurationAction The action that configures the build type.
     *
     * @sample
     * ```kotlin
     * val myContainer = BuildPublishDomainObjectContainer(MyType::class.java, project.objects)
     * extension.buildVariant("release", myContainer) {
     *     // Configuration specific to release builds
     *     someProperty = "release-value"
     *     enableOptimizations = true
     * }
     * ```
     */
    protected fun <T : Any> buildVariant(
        buildVariant: String,
        container: BuildPublishDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Applies build-type specific configuration to a base domain container using the Closure syntax.
     *
     * This method registers a configuration action that will be applied only to the specified
     * build variant using the [BuildPublishDomainObjectContainer] abstraction. It's particularly
     * useful when working with custom domain object containers that require special handling.
     *
     * @param T The type of objects in the container.
     * @param buildVariant The name of the build variant (e.g., "debug", "release").
     * @param container The base domain container to configure.
     * @param configurationClosure The closure that configures the build type.
     *
     **/
    protected fun <T : Any> buildVariant(
        buildVariant: String,
        container: BuildPublishDomainObjectContainer<T>,
        @DelegatesTo(
            genericTypeIndex = 0,
            strategy = Closure.DELEGATE_FIRST,
        )
        configurationAction: Closure<in T>,
    ) {
        container.buildVariant(buildVariant, configurationAction)
    }

    /**
     * Configures the extension with the given project, input, and build variant.
     *
     * This method is called during the configuration phase to set up the extension for a specific
     * build variant. Subclasses should override this method to provide their specific configuration
     * logic based on the provided parameters.
     *
     * The default implementation does nothing, allowing subclasses to implement only the configuration
     * they need.
     *
     * @param project The Gradle project being configured. Provides access to project properties,
     *               extensions, and other Gradle APIs.
     * @param input The extension input containing configuration data from the build script.
     * @param variant The Android application variant being configured.
     *
     * @sample
     * ```kotlin
     * override fun configure(project: Project, input: ExtensionInput, variant: ApplicationVariant) {
     *     // Configure the extension based on the build variant
     *     if (variant.buildType.name == "debug") {
     *         // Debug-specific configuration
     *     } else {
     *         // Release or other build type configuration
     *     }
     * }
     * ```
     */
    open fun configure(
        project: Project,
        input: ExtensionInput,
        variant: ApplicationVariant,
    ) = Unit
}
