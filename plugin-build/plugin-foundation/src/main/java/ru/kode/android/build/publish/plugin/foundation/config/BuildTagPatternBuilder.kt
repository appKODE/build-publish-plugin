package ru.kode.android.build.publish.plugin.foundation.config

import java.util.regex.Pattern

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
     * Adds a literal string (escaped for regex)
     * */
    fun literal(value: String): BuildTagPatternBuilder {
        parts += Regex.escape(value)
        return this
    }

    /**
     * Adds a separator (escaped for regex), e.g. "-", "_"
     * */
    fun separator(value: String): BuildTagPatternBuilder {
        parts += Regex.escape(value)
        return this
    }

    /**
     * Captures a numeric build number: (\d+)
     * */
    fun buildNumber(): BuildTagPatternBuilder {
        parts += "(\\d+)"
        return this
    }

    /**
     * Inserts a build type placeholder: %s
     * */
    fun buildType(): BuildTagPatternBuilder {
        parts += "%s"
        return this
    }

    /**
     * Matches any text ending with a dot: .+\.
     * */
    fun anyBeforeDot(): BuildTagPatternBuilder {
        parts += ".+\\."
        return this
    }

    /**
     * Builds the final regex template with validation
     * */
    fun build(): String {
        val template = parts.joinToString("")

        require(template.contains("(\\d+)")) {
            "Tag pattern must contain a version capture group (e.g. (\\d+))"
        }

        require(template.contains("%s")) {
            "Tag pattern must contain a variant placeholder (%s)"
        }

        val testRegex = template.format("dummyVariant")
        try {
            Pattern.compile(testRegex)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid regex produced: $testRegex", e)
        }

        return template
    }
}
