package ru.kode.android.build.publish.plugin.core.strategy

import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot

/**
 * Strategy interface for formatting commit messages in the changelog.
 *
 * Implementations of this interface define how individual commit messages
 * are transformed before being included in the generated changelog.
 */
interface ChangelogMessageStrategy {
    /**
     * Builds a formatted changelog message from a raw commit message.
     *
     * @param message The original commit message to format.
     * @param commitMessageKey The key used to identify changelog-relevant commits (e.g., `[changelog]`).
     * @param tagSnapshot The build tag snapshot providing context about the current build.
     *
     * @return The formatted message string ready for inclusion in the changelog.
     */
    fun build(
        message: String,
        commitMessageKey: String,
        tagSnapshot: BuildTagSnapshot,
    ): String
}

/**
 * Changelog strategy that keeps the commit message key.
 *
 * Example:
 * Input: "[changelog] Fix crash on startup"
 * Output: "• [changelog] Fix crash on startup"
 */
object KeyPreservingChangelogMessageStrategy : ChangelogMessageStrategy {
    override fun build(
        message: String,
        commitMessageKey: String,
        tagSnapshot: BuildTagSnapshot,
    ): String {
        return "• $message".trim()
    }
}

/**
 * Changelog strategy that removes the commit message key.
 *
 * Example:
 * Input: "[changelog] Fix crash on startup"
 * Output: "• Fix crash on startup"
 */
object KeyRemovingChangelogMessageStrategy : ChangelogMessageStrategy {
    override fun build(
        message: String,
        commitMessageKey: String,
        tagSnapshot: BuildTagSnapshot,
    ): String {
        val cleanMessage =
            message
                .replace(Regex("\\s*$commitMessageKey:?\\s*"), "")
                .trim()

        return "• $cleanMessage".trim()
    }
}
