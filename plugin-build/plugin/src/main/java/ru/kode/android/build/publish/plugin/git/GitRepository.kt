package ru.kode.android.build.publish.plugin.git

import ru.kode.android.build.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.build.publish.plugin.git.entity.Tag
import ru.kode.android.build.publish.plugin.git.entity.TagRange

internal class GitRepository(
    private val commandExecutor: ShellCommandExecutor,
    private val buildVariants: Set<String>,
) {
    /**
     * Finds a range of build tags, returning `null` if no build tags are present (happens on a new projects)
     */
    fun findTagRange(buildVariant: String): TagRange? {
        val tags = commandExecutor.findBuildTags(buildVariant, limitResultCount = 2)
        return if (tags != null) {
            val currentBuildTag = tags.first()
            val previousBuildTag = tags.getOrNull(1)
            TagRange(
                currentBuildTag = Tag.Build(currentBuildTag, buildVariants),
                previousBuildTag = previousBuildTag?.let { Tag.Build(it, buildVariants) }
            )
        } else null
    }

    fun findRecentBuildTag(): Tag.Build? {
        val tags = commandExecutor.findBuildTags(buildVariants, limitResultCount = 1)
        return tags?.first()?.let { Tag.Build(it, buildVariants) }
    }
}
