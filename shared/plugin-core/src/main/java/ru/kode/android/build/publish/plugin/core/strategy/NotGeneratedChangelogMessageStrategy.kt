package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.Tag

/**
 * Strategy interface for generating messages when changelog generation fails or produces no output.
 *
 * Implementations of this interface define how to format messages when the changelog
 * generation process could not produce a meaningful changelog.
 *
 * @see Tag.Build For information about the build tag structure
 */
interface NotGeneratedChangelogMessageStrategy {
    /**
     * Builds a formatted message indicating that the changelog was not generated.
     *
     * @param buildTagPattern The pattern used to identify build tags.
     * @param currentBuildTag The current build tag for which changelog generation was attempted.
     *
     * @return The formatted message string explaining why the changelog was not generated.
     */
    fun build(
        buildTagPattern: String,
        currentBuildTag: Tag.Build,
    ): String
}

/**
 * Default implementation of [NotGeneratedChangelogMessageStrategy] that provides
 * a user-friendly message when changelog generation fails.
 *
 * This object generates a message indicating that no meaningful changes were found
 * for the specified build tag pattern and current build.
 */
object NoChangesNotGeneratedChangelogMessageStrategy : NotGeneratedChangelogMessageStrategy {
    /**
     * Builds a formatted message indicating that the changelog was not generated.
     *
     * This implementation returns a user-friendly message with emoji formatting,
     * including the build tag pattern and current build tag name to help identify
     * the context of the failed changelog generation.
     *
     * @param buildTagPattern The pattern used to identify build tags.
     * @param currentBuildTag The current build tag for which changelog generation was attempted.
     * @return A formatted message string with emoji and markdown formatting.
     */
    override fun build(
        buildTagPattern: String,
        currentBuildTag: Tag.Build,
    ): String {
        return """
            🧩 *Changelog not generated*
            Pattern: `$buildTagPattern`
            Build: `${currentBuildTag.name}`

            No meaningful changes were found for this build.
            """.trimIndent()
    }
}
