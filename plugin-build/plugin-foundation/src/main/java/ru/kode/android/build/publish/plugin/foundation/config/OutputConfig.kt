package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.builder.BuildTagPatternBuilder
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy

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
     * When enabled, the plugin will use default version code (1) and version name (0.0)
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
     * The strategy used to convert version code and name to a meaningful version name.
     *
     * This strategy is used to transform version code and name extracted from Git tags to a meaningful
     * version name. The strategy is applied when generating the version name for the build output.
     *
     * @see VersionNameStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val versionNameStrategy: Property<VersionNameStrategy>

    /**
     * The strategy used to convert version code extracted from Git tags to a meaningful version code.
     *
     * This strategy is used to transform version code extracted from Git tags to a meaningful
     * version code. The strategy is applied when generating the version code for the build output.
     *
     * @see VersionCodeStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val versionCodeStrategy: Property<VersionCodeStrategy>

    /**
     * The strategy used to generate the name of the output APK file.
     *
     * This strategy is used to generate the name of the output APK file based on the build output
     * configuration. The strategy is applied when generating the output APK file name.
     *
     * @see OutputApkNameStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val outputApkNameStrategy: Property<OutputApkNameStrategy>

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

    /**
     * Configures the version name mapper used to convert version code and name to a meaningful version name.
     *
     * @param action The configuration action for [VersionNameStrategy].
     *
     * @see VersionNameStrategy
     */
    fun versionNameStrategy(action: () -> VersionNameStrategy) {
        versionNameStrategy.set(action())
    }

    /**
     * Configures the version code mapper used to convert version code extracted from Git tags to a meaningful version code.
     *
     * @param action The configuration action for [VersionCodeStrategy].
     *
     * @see VersionCodeStrategy
     */
    fun versionCodeStrategy(action: () -> VersionCodeStrategy) {
        versionCodeStrategy.set(action())
    }

    /**
     * Configures the strategy used to generate the final APK output file name.
     *
     * The strategy may include version/tag metadata (if enabled by [useVersionsFromTag]) and the
     * user-provided [baseFileName].
     *
     * @param action Supplier that returns the desired [OutputApkNameStrategy] implementation.
     *
     * @see OutputApkNameStrategy
     */
    fun outputApkNameStrategy(action: () -> OutputApkNameStrategy) {
        outputApkNameStrategy.set(action())
    }
}
