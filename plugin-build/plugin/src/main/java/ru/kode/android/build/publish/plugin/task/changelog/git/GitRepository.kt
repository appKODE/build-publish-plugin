package ru.kode.android.build.publish.plugin.task.changelog.git

import ru.kode.android.build.publish.plugin.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.enity.Tag
import ru.kode.android.build.publish.plugin.enity.TagRange

internal class GitRepository(
    private val gitCommandExecutor: GitCommandExecutor,
) {
    /**
     * Finds a range of build tags, returning `null` if no build tags are present (happens on a new projects)
     */
    fun findTagRange(
        buildVariant: String,
        buildTagPattern: String?,
    ): TagRange? {
        val buildTagRegex = Regex(buildTagPattern ?: DEFAULT_TAG_PATTERN.format(buildVariant))
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
        buildTagPattern: String?,
    ): Tag.Build? {
        val buildTagRegex = Regex(buildTagPattern ?: DEFAULT_TAG_PATTERN.format(buildVariant))
        val tags = gitCommandExecutor.findBuildTags(buildTagRegex, limitResultCount = 1)
        return tags?.first()?.let { Tag.Build(it, buildVariant) }
    }
}

private const val DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"
