package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration interface for build output settings.
 *
 * This interface defines the configuration options for build outputs, including versioning
 * and naming conventions. It's used to customize how build artifacts are named and versioned
 * during the build process.
 */
abstract class OutputConfig {
    abstract val name: String

    /**
     * The base file name prefix for generated output files.
     *
     * This value is used as a prefix for generated APK/AAB files.
     *
     * Example: `"my-app-android"` would result in files like `my-app-android-debug.apk`
     */
    @get:Input
    abstract val baseFileName: Property<String>

    /**
     * Whether to extract version information from Git tags.
     *
     * When enabled, the plugin will extract version code and version name from Git tags.
     * This is useful for maintaining consistent versioning across builds.
     *
     * If disabled, you must provide version information through other means (e.g., gradle.properties).
     * When disabled and [useDefaultsForVersionsAsFallback] is true, default values will be used.
     *
     * @see [Gradle Build Cache Optimization](https://youtu.be/7ll-rkLCtyk?si=Qv_LS0weiYCBT0OV&t=943)
     *
     * Default: `true`
     */
    @get:Input
    @get:Optional
    abstract val useVersionsFromTag: Property<Boolean>

    /**
     * Whether to use stub values when a Git tag is not found.
     *
     * When enabled and no matching Git tag is found, the build will not fail but instead
     * use stub values for version information.
     *
     * Default: `true`
     */
    @get:Input
    @get:Optional
    abstract val useStubsForTagAsFallback: Property<Boolean>

    /**
     * Whether to use default version values when [useVersionsFromTag] is false.
     *
     * When enabled, the plugin will use default version code (1) and version name (1.0)
     * when version information cannot be obtained from Git tags.
     *
     * Default: `true`
     */
    @get:Input
    @get:Optional
    abstract val useDefaultsForVersionsAsFallback: Property<Boolean>

    /**
     * The pattern used to match Git tags for version extraction.
     *
     * This pattern is used to filter Git tags when extracting version information.
     * The pattern should follow Java regex syntax.
     *
     * Example: `".+\\.(\\d+)-%s"` to match tags like v1.2.3-debug
     *
     * @see java.util.regex.Pattern
     */
    @get:Input
    @get:Optional
    internal abstract val buildTagPattern: Property<String>

    /**
     * Configures the pattern used to match Git tags for version extraction.
     *
     * @param action The configuration action for [BuildTagPatternBuilder].
     *
     * @see java.util.regex.Pattern
     * @see BuildTagPatternBuilder
     */
    fun buildTagPattern(action: BuildTagPatternBuilder.() -> Unit) {
        val builder = BuildTagPatternBuilder().apply(action)
        buildTagPattern.set(builder.build())
    }
}
