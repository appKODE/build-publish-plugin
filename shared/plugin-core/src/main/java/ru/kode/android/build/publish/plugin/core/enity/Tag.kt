package ru.kode.android.build.publish.plugin.core.enity

import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.messages.noVariantMessage

private val NUMBER_REGEX = Regex("\\d+")

/**
 * Represents a Git tag with its associated metadata.
 *
 * This sealed class serves as the base for different types of Git tags used throughout the build system.
 * It provides common properties like the tag name, commit SHA, and optional message.
 */
sealed class Tag {
    /**
     * The name of the tag (e.g., "v1.0.0")
     */
    abstract val name: String

    /**
     * The SHA-1 hash of the commit this tag points to
     */
    abstract val commitSha: String

    /**
     * The annotated tag message, or null if this is a lightweight tag
     */
    abstract val message: String?

    /**
     * Represents a generic Git tag without any special build-related metadata.
     *
     * This is used for tags that don't follow the build versioning pattern
     * and don't need special processing.
     */
    data class Generic(
        /**
         * The name of the tag
         */
        override val name: String,
        /**
         * The SHA-1 hash of the tagged commit
         */
        override val commitSha: String,
        /**
         * The annotated tag message, or null for lightweight tags
         */
        override val message: String?,
    ) : Tag()

    /**
     * Represents a build-specific Git tag with version and variant information.
     *
     * This class extends [Tag] with build-specific metadata extracted from the tag name,
     * following the pattern: `{prefix}.{buildNumber}-{variant}`
     */
    data class Build(
        /**
         * The full tag name (e.g., "app.42-debug")
         */
        override val name: String,
        /**
         * The SHA-1 hash of the tagged commit
         */
        override val commitSha: String,
        /**
         * The annotated tag message, or null for lightweight tags
         */
        override val message: String?,
        /**
         * The parsed build version (e.g., "1.0")
         */
        val buildVersion: String,
        /**
         * The build variant (e.g., "debug", "release")
         */
        val buildVariant: String,
        /**
         *  The build number extracted from the tag (e.g. "1")
         */
        val buildNumber: Int,
    ) : Tag() {
        /**
         * Creates a [Build] tag from a generic [Tag] by extracting build metadata.
         *
         * @param tag The source tag to convert
         * @param buildVariant The expected build variant (e.g., "debug", "release")
         *
         * @throws GradleException if the tag doesn't match the expected format or variant
         */
        constructor(tag: Tag, buildVariant: String) : this(
            tag.name,
            tag.commitSha,
            tag.message,
            buildVersion = tag.name.toBuildVersion(),
            buildVariant = tag.toBuildVariant(buildVariant),
            buildNumber = tag.name.toBuildNumber(),
        )
    }
}

/**
 * Extracts the build variant from the tag name.
 *
 * @param buildVariant The expected build variant to validate against the tag name
 *
 * @return The validated build variant
 * @throws GradleException if the tag name doesn't contain the expected build variant
 */
private fun Tag.toBuildVariant(buildVariant: String): String {
    return if (this.name.contains(buildVariant)) {
        buildVariant
    } else {
        throw GradleException(noVariantMessage(buildVariant))
    }
}

/**
 * Extracts the build version from the tag name.
 *
 * TODO: Make version extraction configurable
 *
 * @return The extracted build version
 * @throws UnsupportedOperationException if version extraction is not implemented
 */
private fun String.toBuildVersion(): String {
    return NUMBER_REGEX
        .findAll(substringBefore('-'))
        .map { it.value }
        .toList()
        .dropLast(1)
        .joinToString(".")
}

/**
 * Extracts the build number from the tag name.
 *
 * TODO: Make build number extraction configurable
 *
 * @return The extracted build number
 * @throws NumberFormatException if the build number cannot be parsed as an integer
 */
private fun String.toBuildNumber(): Int {
    return NUMBER_REGEX
        .findAll(substringBefore('-'))
        .last()
        .value
        .toInt()
}
