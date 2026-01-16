package ru.kode.android.build.publish.plugin.core.util

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import ru.kode.android.build.publish.plugin.core.enity.CommitRange
import ru.kode.android.build.publish.plugin.core.enity.Tag
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.ajoberstar.grgit.Tag as GrgitTag

private const val UNKNOWN_COMMIT_INDEX = -1

/**
 * Retrieves a list of Git commits within the specified range, or all commits if the range is null.
 *
 * If the range's sha1 is null, the method retrieves all commits starting from the commit with the
 * specified sha2. Otherwise, it retrieves commits within the specified range.
 *
 * @param range The range of commits to retrieve (null for all commits)
 *
 * @return A list of [Commit] objects within the specified range
 * @throws org.eclipse.jgit.api.errors.GitAPIException If there's an error accessing the Git repository
 */
fun Grgit.getCommitsByRange(range: CommitRange?): List<Commit> {
    return when {
        range == null -> this.log()
        range.sha1 == null -> {
            val commits = this.log()
            val lastCommitIndex = commits.indexOfFirst { it.id == range.sha2 }
            return if (lastCommitIndex == UNKNOWN_COMMIT_INDEX) {
                commits
            } else {
                commits.subList(lastCommitIndex, commits.size)
            }
        }

        else -> this.log { options -> options.range(range.sha1, range.sha2) }
    }
}

fun Commit.utcDateTime(): ZonedDateTime = this.dateTime.withZoneSameLocal(ZoneOffset.UTC)

/**
 * Extracts the build number from this [GrgitTag]'s name using the provided regular expression.
 */
fun GrgitTag.getBuildNumber(regex: Regex): Int {
    return this.name.getBuildNumber(regex)
}

/**
 * Extracts the build number from this [Tag]'s name using the provided regular expression.
 */
fun Tag.getBuildNumber(regex: Regex): Int {
    return this.name.getBuildNumber(regex)
}

/**
 * Extracts the build number from this string using the provided regular expression.
 */
fun String.getBuildNumber(regex: Regex): Int {
    return this.extractIntBy(regex)
}

/**
 * Extracts an integer value from the string using the given [regex].
 *
 * The [regex] must include a **capturing group ( )** that isolates the numeric portion
 * of the string. The first captured group (`groupValues[1]`) is parsed as an integer.
 *
 * If no match or a non-numeric capture is found, the method returns `0`.
 * It also logs the matched parts for debugging purposes.
 *
 * @param regex The [Regex] pattern used to extract the integer.
 * @return The extracted integer, or `0` if extraction fails.
 */
private fun String.extractIntBy(regex: Regex): Int {
    return regex.find(this)?.groupValues
        ?.get(1)
        ?.toIntOrNull()
        ?: 0
}
