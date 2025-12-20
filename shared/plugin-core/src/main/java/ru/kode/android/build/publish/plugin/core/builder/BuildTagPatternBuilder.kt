package ru.kode.android.build.publish.plugin.core.builder

import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.messages.invalidRegexMessage
import ru.kode.android.build.publish.plugin.core.messages.tagPatterMustContainVariantNameMessage
import ru.kode.android.build.publish.plugin.core.messages.tagPatternMustContainVersionGroupMessage
import java.util.regex.Pattern

private const val BUILD_VERSION_REGEX_PART = "(\\d+)"
private const val BUILD_VARIANT_NAME_REGEX_PART = "%s"
private const val ANY_BEFORE_DOT_REGEX_PART = ".+\\."
private const val ANY_OPTIONAL_SYMBOLS_PART = "([A-Za-z0-9]+)?"

/**
 * Builder for constructing a regular expression pattern for matching build tags.
 *
 * The resulting pattern can capture version numbers and build variants.
 * It should be used to configure [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask] with the correct pattern.
 *
 * @see ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask
 */
class BuildTagPatternBuilder {
    private val parts = mutableListOf<String>()

    /**
     * Adds a literal string (not escaped)
     */
    fun literal(value: String): BuildTagPatternBuilder {
        parts += value
        return this
    }

    /**
     * Adds a separator (escaped for regex), e.g. "-", "_", "+" and etc.
     */
    fun separator(value: String): BuildTagPatternBuilder {
        parts += Regex.escape(value)
        return this
    }

    /**
     * Adds an optional separator (escaped for regex), e.g. "-", "_", "+" and etc.
     */
    fun optionalSeparator(value: String): BuildTagPatternBuilder {
        parts += "(${Regex.escape(value)})?"
        return this
    }

    /**
     * Captures a numeric build version: (\d+)
     */
    fun buildVersion(): BuildTagPatternBuilder {
        parts += BUILD_VERSION_REGEX_PART
        return this
    }

    /**
     * Inserts a build variant name placeholder: %s
     */
    fun buildVariantName(): BuildTagPatternBuilder {
        parts += BUILD_VARIANT_NAME_REGEX_PART
        return this
    }

    /**
     * Matches any text ending with a dot: .+\.
     */
    fun anyBeforeDot(): BuildTagPatternBuilder {
        parts += ANY_BEFORE_DOT_REGEX_PART
        return this
    }

    /**
     * Matches a sequence of optional alphanumeric characters (like "androidAuto" or "tv").
     */
    fun anyOptionalSymbols(): BuildTagPatternBuilder {
        parts += ANY_OPTIONAL_SYMBOLS_PART
        return this
    }

    /**
     * Builds the final regex template with validation
     */
    @Suppress("TooGenericExceptionCaught", "ThrowsCount") // Need to handle all exceptions
    fun build(): String {
        val template = parts.joinToString("")

        if (!template.contains(BUILD_VERSION_REGEX_PART)) {
            throw GradleException(
                tagPatternMustContainVersionGroupMessage(BUILD_VERSION_REGEX_PART),
            )
        }

        if (!template.contains(BUILD_VARIANT_NAME_REGEX_PART)) {
            throw GradleException(
                tagPatterMustContainVariantNameMessage(BUILD_VARIANT_NAME_REGEX_PART),
            )
        }

        val testRegex = template.format("dummyVariant")
        try {
            Pattern.compile(testRegex)
        } catch (e: Exception) {
            throw GradleException(invalidRegexMessage(testRegex), e)
        }

        return template
    }
}
