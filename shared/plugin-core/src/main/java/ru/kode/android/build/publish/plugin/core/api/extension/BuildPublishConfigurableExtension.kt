package ru.kode.android.build.publish.plugin.core.api.extension

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.entity.ExtensionInput
import ru.kode.android.gradle.commons.api.extension.PluginConfigurableExtension

/**
 * Base class for configurable extensions in the build and publish plugin system.
 *
 * Adds the build-publish-specific [configure] hook on top of [PluginConfigurableExtension]'s
 * generic `common`/`buildVariant` DSL plumbing.
 */
open class BuildPublishConfigurableExtension : PluginConfigurableExtension() {
    /**
     * Configures the extension with the given project, input, and build variant.
     *
     * This method is called during the configuration phase to set up the extension for a specific
     * build variant. Subclasses should override this method to provide their specific configuration
     * logic based on the provided parameters.
     *
     * The default implementation does nothing, allowing subclasses to implement only the configuration
     * they need.
     */
    open fun configure(
        project: Project,
        input: ExtensionInput,
        variant: ApplicationVariant,
    ) = Unit
}
