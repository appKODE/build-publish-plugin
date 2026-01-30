package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.Tag

/**
 * Strategy interface for generating changelog messages when no changes are detected.
 *
 * Implementations of this interface define how to format messages when the changelog
 * generation process finds no meaningful changes between builds.
 *
 * @see BuildTagSnapshot For information about the tag snapshot structure
 */
interface EmptyChangelogMessageStrategy {
    /**
     * Builds a changelog message based on the provided tag snapshot.
     *
     * This method determines the appropriate message to display when no changes
     * are detected, considering whether there is a previous build tag or not.
     *
     * @param tagSnapshot The snapshot containing current and previous build tag information
     * @return A formatted string message indicating no changes were detected
     */
    fun build(tagSnapshot: BuildTagSnapshot): String

    /**
     * Generates a message indicating no changes since a previous build tag.
     *
     * @param tag The previous build tag to reference in the message
     * @return A formatted string message referencing the previous tag
     */
    fun noChangesSincePreviousTagMessage(tag: Tag.Build): String

    /**
     * Generates a message indicating no changes since the start of the repository.
     *
     * This is typically used when there is no previous build tag available,
     * indicating this may be the first build.
     *
     * @return A formatted string message for the initial state
     */
    fun noChangesSinceStartMessage(): String
}

/**
 * Default implementation of [EmptyChangelogMessageStrategy] that provides
 * user-friendly messages when no changes are detected in the changelog.
 *
 * This object generates different messages based on whether a previous
 * build tag exists:
 * - If a previous tag exists, it indicates no changes since that tag
 * - If no previous tag exists, it indicates this is the starting point
 */
object NoChangesChangelogMessageStrategy : EmptyChangelogMessageStrategy {
    /**
     * Builds the appropriate no-changes message based on the tag snapshot.
     *
     * @param tagSnapshot The snapshot containing current and previous build tag information
     * @return A formatted message indicating no changes, with context about the previous tag if available
     */
    override fun build(tagSnapshot: BuildTagSnapshot): String {
        val previousBuildTag = tagSnapshot.previous
        return if (previousBuildTag != null) {
            noChangesSincePreviousTagMessage(previousBuildTag)
        } else {
            noChangesSinceStartMessage()
        }
    }

    /**
     * Generates a message indicating no changes since the specified previous build tag.
     *
     * @param tag The previous build tag to reference
     * @return A formatted message indicating no changes since the given tag
     */
    override fun noChangesSincePreviousTagMessage(tag: Tag.Build): String {
        return noChangesDetectedSinceBuildMessage(tag.name)
    }

    /**
     * Generates a message indicating no changes since the repository start.
     *
     * @return A formatted message for the initial repository state
     */
    override fun noChangesSinceStartMessage(): String {
        return noChangedDetectedSinceStartMessage()
    }
}

/**
 * Default changelog message used when there is no previous build tag.
 *
 * This message is displayed when the changelog generation process cannot find
 * a previous build tag, typically indicating this is the first build of the project.
 *
 * @return A formatted string with emoji and markdown formatting indicating the starting point
 */
fun noChangedDetectedSinceStartMessage(): String {
    return """
        🌱 *No changes detected*
        _Starting point of the repository_

        There are no commits to include yet.
        This usually means this is the first build.
        """.trimIndent()
}

/**
 * Default changelog message used when there are no changes since a previous build tag.
 *
 * This message is displayed when the changelog generation process finds no new
 * commits or configuration updates between the current and previous build tags.
 *
 * @param tagName The previous build tag that was used as the comparison point
 * @return A formatted string with emoji and markdown formatting referencing the previous tag
 */
fun noChangesDetectedSinceBuildMessage(tagName: String): String {
    return """
        🔁 *No changes detected*
        _Since build `$tagName`_

        No new commits or configuration updates were found.
        """.trimIndent()
}
