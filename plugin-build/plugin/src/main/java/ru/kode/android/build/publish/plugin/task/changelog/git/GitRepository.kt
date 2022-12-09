package ru.kode.android.build.publish.plugin.task.changelog.git

import ru.kode.android.build.publish.plugin.command.GitCommandExecutor
import ru.kode.android.build.publish.plugin.enity.Tag
import ru.kode.android.build.publish.plugin.enity.TagRange

internal class GitRepository(
    private val gitCommandExecutor: GitCommandExecutor,
    private val buildVariants: Set<String>,
) {
    /**
     * Finds a range of build tags, returning `null` if no build tags are present (happens on a new projects)
     */
    fun findTagRange(buildVariant: String): TagRange? {
        val tags = gitCommandExecutor.findBuildTags(setOf(buildVariant), limitResultCount = 2)
        return if (tags != null) {
            val currentBuildTag = tags.first()
            val previousBuildTag = tags.getOrNull(1)
            TagRange(
                // todo add Build types
                currentBuildTag = Tag.Build(currentBuildTag, buildVariants),
                previousBuildTag = previousBuildTag?.let { Tag.Build(it, buildVariants) }
            )
        } else null
    }

    fun findRecentBuildTag(): Tag.Build? {
        val tags = gitCommandExecutor.findBuildTags(buildVariants, limitResultCount = 1)
        return tags?.first()?.let { Tag.Build(it, buildVariants) }
    }
}
