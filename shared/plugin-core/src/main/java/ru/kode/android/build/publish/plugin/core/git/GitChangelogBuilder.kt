package ru.kode.android.build.publish.plugin.core.git

import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.IssueReference
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.messages.buildingChangelogForTagRangeMessage
import ru.kode.android.build.publish.plugin.core.messages.unresolvedIssueReferenceMessage
import ru.kode.android.build.publish.plugin.core.strategy.ResolvedIssueStrategy
import ru.kode.android.build.publish.plugin.core.strategy.UnresolvedIssueStrategy

/**
 * Builds changelogs by extracting and formatting commit messages from Git history.
 *
 * This class provides functionality to generate human-readable changelogs by analyzing
 * Git commit messages between tags. It supports custom message keys and fallback values.
 *
 * @property gitRepository The Git repository to extract commit history from
 * @property logger Logger for warning and debug messages
 *
 * @see GitRepository For repository operations used to gather commit data
 */
class GitChangelogBuilder(
    private val gitRepository: GitRepository,
    private val logger: PluginLogger,
) {
    /**
     * Builds a changelog string for a given build tag by determining the tag range and collecting
     * commit messages between the tags.
     *
     * This function first attempts to find a [BuildTagSnapshot] between the specified [tagSnapshot] and the
     * previous matching build tag using [buildTagPattern]. If no valid tag range is found, a warning
     * is logged and `null` is returned.
     *
     * If a valid tag range is found, it collects commit messages within that range and filters them
     * using the provided [messageKey]. The resulting commits are then passed to [buildChangelog] to
     * generate the changelog content.
     *
     * If [excludeKey] is `true`, occurrences of the [messageKey] (for example `[changelog]`) are
     * stripped from commit messages in the generated changelog output. If `false`, the key remains
     * visible in the resulting changelog.
     *
     * If no changelog content is generated, and [defaultValueSupplier] is provided, the supplier is
     * invoked to provide a fallback changelog string.
     *
     * @param messageKey the key used to identify which commit messages to include in the changelog.
     * @param excludeKey whether to remove the [messageKey] from commit messages in the generated changelog.
     * @param tagSnapshot the build tag for which the changelog is being generated.
     * @param buildTagPattern the pattern used to find related build tags and determine the tag range.
     * @param defaultValueSupplier an optional function that supplies a default changelog string
     *        when no changelog can be built; receives the [TagRange] as input.
     *
     * @return the generated changelog string, the value from [defaultValueSupplier] if provided,
     *         or `null` if no tag range could be determined or no changelog could be built.
     */
    @Suppress("ReturnCount", "LongParameterList")
    fun buildForSnapshot(
        messageKey: String,
        annotatedTagMessageBuilder: (String) -> String,
        messageBuilder: (String) -> String,
        tagSnapshot: BuildTagSnapshot,
        issueReferences: List<IssueReference>,
        resolvers: List<IssueResolver>,
        resolvedStrategy: ResolvedIssueStrategy,
        unresolvedStrategy: UnresolvedIssueStrategy,
    ): String? {
        logger.info(buildingChangelogForTagRangeMessage(tagSnapshot))
        return buildChangelog(
            snapshot = tagSnapshot,
            annotatedTagMessageBuilder = annotatedTagMessageBuilder,
            commitMessagesResolver = {
                val manualLines =
                    gitRepository.markedCommitMessages(messageKey, tagSnapshot).map(messageBuilder)
                manualLines +
                    buildReferenceLines(
                        messageKey = messageKey,
                        tagSnapshot = tagSnapshot,
                        issueReferences = issueReferences,
                        resolvers = resolvers,
                        resolvedStrategy = resolvedStrategy,
                        unresolvedStrategy = unresolvedStrategy,
                        manualLines = manualLines,
                    )
            },
        )
    }

    /**
     * Builds the changelog lines derived from `CLOSES`/`FIXES` issue references: each reference token is
     * resolved via the first matching [resolvers] and rendered by [resolvedStrategy]; unresolved tokens are
     * rendered by [unresolvedStrategy] (or skipped). Resolution never throws — a failing resolver is treated
     * as "unresolved". Tokens already covered by a manual `CHANGELOG:` entry (present in [manualLines]) and
     * duplicate keys are dropped.
     */
    @Suppress("LongParameterList")
    private fun buildReferenceLines(
        messageKey: String,
        tagSnapshot: BuildTagSnapshot,
        issueReferences: List<IssueReference>,
        resolvers: List<IssueResolver>,
        resolvedStrategy: ResolvedIssueStrategy,
        unresolvedStrategy: UnresolvedIssueStrategy,
        manualLines: List<String>,
    ): List<String> {
        if (issueReferences.isEmpty() || resolvers.isEmpty() || tagSnapshot.pointSameCommit) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        val seenTokens = mutableSetOf<String>()
        val seenKeys = mutableSetOf<String>()

        gitRepository.issueReferenceLines(issueReferences, messageKey, tagSnapshot).forEach { commit ->
            commit.referenceLines.forEach { line ->
                val rendered =
                    renderReferenceLine(
                        line = line,
                        commitChangelogLine = commit.changelogLine,
                        issueReferences = issueReferences,
                        resolvers = resolvers,
                        resolvedStrategy = resolvedStrategy,
                        unresolvedStrategy = unresolvedStrategy,
                        manualLines = manualLines,
                        seenTokens = seenTokens,
                        seenKeys = seenKeys,
                    ) ?: return@forEach
                result += rendered
            }
        }
        return result
    }

    /**
     * Renders a single `CLOSES`/`FIXES` reference [line] into a changelog entry (or `null` when the line
     * carries no known marker, its token was already seen, it duplicates a manual entry, or the chosen
     * strategy omits it). [seenTokens]/[seenKeys] are mutated to deduplicate across the whole range.
     */
    @Suppress("LongParameterList", "ReturnCount")
    private fun renderReferenceLine(
        line: String,
        commitChangelogLine: String?,
        issueReferences: List<IssueReference>,
        resolvers: List<IssueResolver>,
        resolvedStrategy: ResolvedIssueStrategy,
        unresolvedStrategy: UnresolvedIssueStrategy,
        manualLines: List<String>,
        seenTokens: MutableSet<String>,
        seenKeys: MutableSet<String>,
    ): String? {
        val reference = issueReferences.firstOrNull { line.contains(it.key) } ?: return null
        val token =
            Regex(reference.numberPattern).find(line.substringAfter(reference.key))?.value ?: return null
        if (!seenTokens.add(token)) return null
        if (manualLines.any { it.contains(token) }) return null

        val resolved =
            resolvers.firstNotNullOfOrNull { resolver ->
                runCatching { resolver.resolve(token) }.getOrNull()
            }
        if (resolved != null) {
            if (!seenKeys.add(resolved.key)) return null
            return resolvedStrategy.build(resolved.key, resolved.title, commitChangelogLine)
        }
        logger.warn(unresolvedIssueReferenceMessage(token))
        if (!seenKeys.add(token)) return null
        return unresolvedStrategy.build(token, commitChangelogLine)
    }

    /**
     * Constructs the final changelog text from the given tag range and commit messages.
     *
     * This method formats the changelog with the annotated tag message (if present) and
     * the list of commit messages, each prefixed with a bullet point.
     *
     * @param snapshot The range of tags to include in the changelog
     * @param commitMessagesResolver Function that provides the list of commit messages
     *
     * @return The formatted changelog string, or null if no messages are found
     */
    private fun buildChangelog(
        snapshot: BuildTagSnapshot,
        annotatedTagMessageBuilder: (String) -> String,
        commitMessagesResolver: () -> List<String>,
    ): String? {
        val messageBuilder =
            StringBuilder().apply {
                val annotatedTagMessage = snapshot.current.message
                if (annotatedTagMessage?.isNotBlank() == true) {
                    appendLine(annotatedTagMessageBuilder(annotatedTagMessage))
                }
            }

        // it can happen that 2 tags point to the same commit, so no extraction of changelog is necessary
        // (but remember, tags can be annotated - which is taken care of above)
        if (!snapshot.pointSameCommit) {
            commitMessagesResolver()
                .forEach { messageBuilder.appendLine(it) }
        }
        return messageBuilder.toString().takeIf { it.isNotBlank() }?.trim()
    }
}
