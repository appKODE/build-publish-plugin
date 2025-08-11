package ru.kode.android.build.publish.plugin.core.git

import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.TagRange

class GitRepository(
    private val gitCommandExecutor: GitCommandExecutor,
) {
    /**
     * Finds a range of build tags, returning `null` if no build tags are present (happens on a new projects)
     */
    fun findTagRange(
        buildVariant: String,
        buildTagPattern: String,
    ): TagRange? {
        val buildTagRegex = Regex(buildTagPattern)
        return gitCommandExecutor.findBuildTags(buildTagRegex, limitResultCount = 2)
            ?.let { tags ->
                val currentBuildTag = tags.first()
                val previousBuildTag = tags.getOrNull(1)
                TagRange(
                    currentBuildTag = Tag.Build(currentBuildTag, buildVariant),
                    previousBuildTag = previousBuildTag?.let { Tag.Build(it, buildVariant) },
                )
            }
    }

    fun findRecentBuildTag(
        buildVariant: String,
        buildTagPattern: String,
    ): Tag.Build? {
        val buildTagRegex = Regex(buildTagPattern)
        val tags = gitCommandExecutor.findBuildTags(buildTagRegex, limitResultCount = 1)
        return tags?.first()?.let { Tag.Build(it, buildVariant) }
    }

    fun markedCommitMessages(
        messageKey: String,
        tagRange: TagRange,
    ): List<String> {
        return gitCommandExecutor
            .extractMarkedCommitMessages(messageKey, tagRange.asCommitRange())
            .map { it.replace(Regex("\\s*$messageKey:?\\s*"), "• ") }
    }
}

const val DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"
