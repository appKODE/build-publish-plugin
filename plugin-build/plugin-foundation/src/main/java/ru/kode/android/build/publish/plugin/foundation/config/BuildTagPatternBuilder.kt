package ru.kode.android.build.publish.plugin.foundation.config

import java.util.regex.Pattern
import org.gradle.api.GradleException

private const val BUILD_VERSION_REGEX_PART = "(\\d+)"
private const val BUILD_VARIANT_NAME_REGEX_PART = "%s"
private const val ANY_BEFORE_DOT_REGEX_PART = ".+\\."

/**
 * Builder for constructing a regular expression pattern for matching build tags.
 *
 * The resulting pattern can capture version numbers and build variants.
 * It should be used to configure [GetLastTagTask] with the correct pattern.
 *
 * @see GetLastTagTask
 */
class BuildTagPatternBuilder {
    private val parts = mutableListOf<String>()

    /**
     * Adds a literal string (not escaped)
     * */
    fun literal(value: String): BuildTagPatternBuilder {
        parts += value
        return this
    }

    /**
     * Adds a separator (escaped for regex), e.g. "-", "_", "+" and etc.
     * */
    fun separator(value: String): BuildTagPatternBuilder {
        parts += Regex.escape(value)
        return this
    }

    /**
     * Captures a numeric build version: (\d+)
     * */
    fun buildVersion(): BuildTagPatternBuilder {
        parts += BUILD_VERSION_REGEX_PART
        return this
    }

    /**
     * Inserts a build variant name placeholder: %s
     * */
    fun buildVariantName(): BuildTagPatternBuilder {
        parts += BUILD_VARIANT_NAME_REGEX_PART
        return this
    }

    /**
     * Matches any text ending with a dot: .+\.
     * */
    fun anyBeforeDot(): BuildTagPatternBuilder {
        parts += ANY_BEFORE_DOT_REGEX_PART
        return this
    }

    /**
     * Builds the final regex template with validation
     * */
    fun build(): String {
        val template = parts.joinToString("")

        if (!template.contains(BUILD_VERSION_REGEX_PART)) {
            throw GradleException(
                "Tag pattern must contain a build version group (e.g. $BUILD_VERSION_REGEX_PART)"
            )
        }

        if (!template.contains(BUILD_VARIANT_NAME_REGEX_PART)) {
            throw GradleException(
                "Tag pattern must contain a build variant name group $BUILD_VARIANT_NAME_REGEX_PART"
            )
        }

        val testRegex = template.format("dummyVariant")
        try {
            Pattern.compile(testRegex)
        } catch (e: Exception) {
            throw GradleException("Invalid regex produced: $testRegex", e)
        }

        return template
    }
}
