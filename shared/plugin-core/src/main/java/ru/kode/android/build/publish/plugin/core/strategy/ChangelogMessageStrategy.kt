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
     * @param excludeMessageKey Whether to remove the [commitMessageKey] from the formatted output.
     * @param tagSnapshot The build tag snapshot providing context about the current build.
     *
     * @return The formatted message string ready for inclusion in the changelog.
     */
    fun build(
        message: String,
        commitMessageKey: String,
        excludeMessageKey: Boolean,
        tagSnapshot: BuildTagSnapshot,
    ): String
}

/**
 * Default implementation of [ChangelogMessageStrategy] that handles message key removal.
 *
 * This strategy formats commit messages as bullet points and optionally removes
 * the commit message key from the output based on the [excludeMessageKey] parameter.
 *
 * Example usage:
 * - Input: `"[changelog] Fix crash on startup"`, `commitMessageKey = "[changelog]"`, `excludeMessageKey = true`
 * - Output: `"• Fix crash on startup"`
 *
 * - Input: `"[changelog] Fix crash on startup"`, `commitMessageKey = "[changelog]"`, `excludeMessageKey = false`
 * - Output: `"• [changelog] Fix crash on startup"`
 */
object KeyAwareChangelogMessageStrategy : ChangelogMessageStrategy {
    /**
     * Formats a commit message as a bullet point, optionally removing the message key.
     *
     * @param message The original commit message to format.
     * @param commitMessageKey The key to optionally remove from the message.
     * @param excludeMessageKey If `true`, the [commitMessageKey] and any trailing colon
     *        are stripped from the message. If `false`, the message is preserved as-is.
     * @param tagSnapshot The build tag snapshot (unused in this implementation but available for context).
     *
     * @return A bullet-pointed changelog entry.
     */
    override fun build(
        message: String,
        commitMessageKey: String,
        excludeMessageKey: Boolean,
        tagSnapshot: BuildTagSnapshot,
    ): String {
        return if (excludeMessageKey) {
            val cleanMessage = message.replace(Regex("\\s*$commitMessageKey:?\\s*"), "").trim()
            "• $cleanMessage".trim()
        } else {
            "• $message".trim()
        }
    }
}
