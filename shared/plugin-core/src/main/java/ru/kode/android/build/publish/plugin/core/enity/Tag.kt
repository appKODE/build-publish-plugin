package ru.kode.android.build.publish.plugin.core.enity

import kotlinx.serialization.Serializable
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.messages.noVariantMessage

/**
 * Regular expression used to match numeric sequences in tag names.
 * This is used for extracting version numbers and build numbers from tag strings.
 */
private val NUMBER_REGEX = Regex("\\d+")

/**
 * Represents a Git tag with its associated metadata in the build system.
 *
 * This sealed class serves as the base for different types of Git tags used throughout the build system.
 * It provides a common interface for accessing tag properties and enforces a consistent structure
 * for all tag types in the system.
 *
 */
@Serializable
sealed class Tag {
    /**
     * The name of the tag (e.g., "v1.0.0", "app.42-debug").
     */
    abstract val name: String

    /**
     * The SHA-1 hash of the commit this tag points to.
     */
    abstract val commitSha: String

    /**
     * The annotated tag message, or null if this is a lightweight tag.
     */
    abstract val message: String?

    /**
     * Represents a generic Git tag without any special build-related metadata.
     *
     * This class is used for tags that don't follow the build versioning pattern
     * and don't require special processing. It serves as a fallback for any tag
     * that doesn't match the expected build tag format.
     *
     */
    @Serializable
    data class Generic(
        /**
         * The full name of the tag (e.g., "v1.0.0", "initial-commit").
         */
        override val name: String,
        /**
         * The SHA-1 hash of the commit this tag references.
         */
        override val commitSha: String,
        /**
         * The annotated tag message, or null if this is a lightweight tag.
         */
        override val message: String?,
    ) : Tag()

    /**
     * Represents a build-specific Git tag with version and variant information.
     *
     * This class extends [Tag] with build-specific metadata extracted from the tag name,
     * following the pattern: `{prefix}.{buildNumber}-{variant}`. It's used to track
     * and manage build versions throughout the CI/CD pipeline.
     *
     */
    @Serializable
    data class Build(
        /**
         * The full tag name (e.g., "app.42-debug").
         */
        override val name: String,
        /**
         * The SHA-1 hash of the commit this tag references.
         */
        override val commitSha: String,
        /**
         * The annotated tag message, or null for lightweight tags.
         */
        override val message: String?,
        /**
         * The parsed semantic version (e.g., "1.0.0") extracted from the tag.
         */
        val buildVersion: String,
        /**
         * The build variant (e.g., "debug", "release") this tag represents.
         */
        val buildVariant: String,
        /**
         * The sequential build number extracted from the tag.
         */
        val buildNumber: Int,
    ) : Tag() {
        /**
         * Creates a [Build] tag from a generic [Tag] by extracting build metadata.
         *
         * This constructor parses the tag name to extract version information and validates
         * that the tag matches the expected build variant.
         *
         * @param tag The source tag to convert. Must be in the format `{prefix}.{buildNumber}-{variant}`.
         * @param buildVariant The expected build variant (e.g., "debug", "release").
         *
         * @throws GradleException if the tag doesn't match the expected format or variant.
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
 * Extracts and validates the build variant from the tag name.
 *
 * This function checks if the tag name contains the expected build variant
 * and returns it if valid. This ensures that tags are correctly associated
 * with their intended build variants.
 *
 * @param buildVariant The expected build variant to validate against the tag name.
 * @return The validated build variant.
 * @throws GradleException if the tag name doesn't contain the expected build variant,
 *                         with a descriptive error message.
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
 * This function parses the tag name to extract the semantic version number.
 * The version is expected to be in the format `{major}.{minor}.{patch}` and is
 * extracted from the part of the tag before the first hyphen.
 *
 * @receiver The tag name string to extract the version from.
 * @return The extracted semantic version as a string.
 * @throws UnsupportedOperationException if version extraction is not implemented.
 *
 * @sample
 * "app.1.2.3.42-debug".toBuildVersion() // returns "1.2.3"
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
 * This function parses the tag name to extract the sequential build number.
 * The build number is expected to be the last numeric sequence before the first hyphen.
 *
 * @receiver The tag name string to extract the build number from.
 * @return The extracted build number as an integer.
 * @throws NumberFormatException if the build number cannot be parsed as an integer.
 *
 * @sample
 * "app.1.2.3.42-debug".toBuildNumber() // returns 42
 */
private fun String.toBuildNumber(): Int {
    return NUMBER_REGEX
        .findAll(substringBefore('-'))
        .last()
        .value
        .toInt()
}
