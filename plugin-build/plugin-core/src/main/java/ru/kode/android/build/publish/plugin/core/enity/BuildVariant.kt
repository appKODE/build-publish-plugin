package ru.kode.android.build.publish.plugin.core.enity

/**
 * Represents a build variant in an Android project.
 *
 * A build variant is a combination of build type and product flavor that Gradle uses to create a build.
 * This class captures the essential information about a build variant including its name, flavor, and build type.
 */
data class BuildVariant(
    /**
     * The full name of the build variant (e.g., "stagingDebug")
     */
    val name: String,
    /**
     * The name of the product flavor, or null if no flavor is configured
     */
    val flavorName: String?,
    /**
     * The name of the build type (e.g., "debug", "release"), or null if not specified
     */
    val buildTypeName: String?,
)
