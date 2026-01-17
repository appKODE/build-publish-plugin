package ru.kode.android.build.publish.plugin.foundation.validate

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.foundation.messages.mustBeUsedWithAndroidMessage
import ru.kode.android.build.publish.plugin.foundation.messages.mustBeUsedWithVersionMessage
import ru.kode.android.build.publish.plugin.foundation.validate.AgpVersions.MIN_VERSION

/**
 * Validates that the plugin is applied to a supported Android project.
 *
 * The foundation plugin requires:
 * - The `com.android.application` plugin to be applied
 * - A minimum Android Gradle Plugin version
 *
 * If the project is not supported, the build is stopped by throwing [StopExecutionException].
 */
@Suppress("ThrowsCount") // block to throws exceptions on apply
internal fun Project.stopExecutionIfNotSupported() {
    val androidComponents =
        extensions.findByType(AndroidComponentsExtension::class.java)
            ?: return

    val current: AndroidPluginVersion = androidComponents.pluginVersion
    val minVersion = MIN_VERSION
    if (current < minVersion) {
        throw StopExecutionException(mustBeUsedWithVersionMessage(minVersion))
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(mustBeUsedWithAndroidMessage())
    }
}

/**
 * Resolved Android Gradle Plugin versions used by the support check.
 */
object AgpVersions {
    val MIN_VERSION = AndroidPluginVersion(7, 4, 0)
}
