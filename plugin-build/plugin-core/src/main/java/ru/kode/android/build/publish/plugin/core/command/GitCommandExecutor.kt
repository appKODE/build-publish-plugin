package ru.kode.android.build.publish.plugin.core.command

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import ru.kode.android.build.publish.plugin.core.enity.CommitRange
import ru.kode.android.build.publish.plugin.core.enity.Tag

class GitCommandExecutor(
    private val grgit: Grgit,
) {
    /**
     * Extracts lines which contain [key] from all commit messages in commit range (inclusive).
     * For example calling this function with arguments `"CHANGELOG", CommitRange("someSHA1", "someSHA2")`
     * will run `git log` command and  extract lines containing "CHANGELOG" from each commit in that range.
     */
    fun extractMarkedCommitMessages(
        key: String,
        range: CommitRange?,
    ): List<String> {
        return getCommitsByRange(range)
            .flatMap { commit ->
                commit.fullMessage
                    .split('\n')
                    .filter { message -> message.contains(key) }
            }
    }

    /**
     * Runs git to find information about tags of the [buildVariant].
     * Resulting set will be limited to [limitResultCount] tags or `null` if no tags found
     */
    fun findBuildTags(
        buildTagRegex: Regex,
        limitResultCount: Int,
    ): List<Tag>? {
        return grgit.tag.list()
            .filter { tag -> tag.name.matches(buildTagRegex) }
            .sortedBy { tag ->
                buildTagRegex.find(tag.name)?.groupValues?.get(1)?.toIntOrNull()
                    ?: error("internal error: failed to parse build number for tag ${tag.name}")
            }
            .map { tag ->
                Tag.Generic(
                    name = tag.name,
                    commitSha = tag.commit.id,
                    message = tag.fullMessage,
                )
            }
            .reversed()
            .take(limitResultCount)
            .ifEmpty { null }
    }

    private fun getCommitsByRange(range: CommitRange?): List<Commit> {
        return when {
            range == null -> grgit.log()
            range.sha1 == null -> {
                val commits = grgit.log()
                val lastCommitIndex = commits.indexOfFirst { it.id == range.sha2 }
                return if (lastCommitIndex == UNKNOWN_COMMIT_INDEX) {
                    commits
                } else {
                    commits.subList(lastCommitIndex, commits.size)
                }
            }

            else -> grgit.log { options -> options.range(range.sha1, range.sha2) }
        }
    }
}

private const val UNKNOWN_COMMIT_INDEX = -1
