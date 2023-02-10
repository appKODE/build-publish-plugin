package ru.kode.android.build.publish.plugin.command

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.gradle.GrgitService
import ru.kode.android.build.publish.plugin.enity.CommitRange
import ru.kode.android.build.publish.plugin.enity.Tag

internal class GitCommandExecutor(
    private val grgitService: GrgitService,
) {
    /**
     * Extracts lines which contain [tag] from all commit messages in commit range (inclusive).
     * For example calling this function with arguments `"CHANGELOG", CommitRange("someSHA1", "someSHA2")`
     * will run `git log` command and  extract lines containing "CHANGELOG" from each commit in that range.
     */
    fun extractTagFromCommitMessages(tag: String, range: CommitRange?): List<String> {
        return getCommitsByRange(range)
            .flatMap { commit ->
                commit.fullMessage
                    .split('\n')
                    .filter { message -> message.contains(tag) }
            }
    }

    /**
     * Runs git to find information about tags of a set of [buildVariants].
     * Resulting set will be limited to [limitResultCount] tags or `null` if no tags found
     */
    fun findBuildTags(buildVariants: Set<String>, limitResultCount: Int): List<Tag>? {
        return grgitService.grgit.tag.list()
            .filter { tag ->
                buildVariants.any { variant ->
                    tag.name.contains("-$variant")
                }
            }
            .map { tag ->
                Tag.Generic(
                    name = tag.name,
                    commitSha = tag.commit.id,
                    message = tag.fullMessage,
                )
            }
            .takeLast(limitResultCount)
            .ifEmpty { null }
    }

    private fun getCommitsByRange(range: CommitRange?): List<Commit> {
        return when {
            range == null -> grgitService.grgit.log()
            range.sha1 == null -> {
                val commits = grgitService.grgit.log()
                val lastCommitIndex = commits.indexOfFirst { it.id == range.sha2 }
                return if (lastCommitIndex == UNKNOWN_COMMIT_INDEX) {
                    commits
                } else {
                    commits.subList(lastCommitIndex, commits.size)
                }
            }
            else -> grgitService.grgit.log { options -> options.range(range.sha1, range.sha2) }
        }
    }
}

private const val UNKNOWN_COMMIT_INDEX = -1
