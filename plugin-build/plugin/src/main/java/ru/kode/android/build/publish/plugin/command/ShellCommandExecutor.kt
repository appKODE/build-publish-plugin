package ru.kode.android.build.publish.plugin.command

import org.gradle.api.Project
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import ru.kode.android.build.publish.plugin.git.entity.CommitRange
import ru.kode.android.build.publish.plugin.git.entity.Tag
import java.io.ByteArrayOutputStream

interface ShellCommandExecutor {
    /**
     * Extracts lines which contain [tag] from all commit messages in commit range (inclusive).
     * For example calling this function with arguments `"CHANGELOG", CommitRange("someSHA1", "someSHA2")`
     * will run `git log` command and  extract lines containing "CHANGELOG" from each commit in that range.
     */
    fun extractTagFromCommitMessages(tag: String, range: CommitRange?): List<String>

    /**
     * Runs git to find information about tags of a set of [buildVariants].
     * Resulting set will be limited to [limitResultCount] tags or `null` if no tags found
     */
    fun findBuildTags(buildVariants: Set<String>, limitResultCount: Int): List<Tag>?

    /**
     * Runs git to find information about tags of a particular build type.
     * Resulting set will be limited to [limitResultCount] tags or `null` if no tags found
     */
    fun findBuildTags(buildVariant: String, limitResultCount: Int): List<Tag>? {
        return findBuildTags(setOf(buildVariant), limitResultCount)
    }

    /**
     * Sends JSON in [jsonBody] to a web-hook at [webHookUrl]
     */
    fun sendToWebHook(webHookUrl: String, jsonBody: String)

    /**
     * Sends JSON in [jsonBody] to a web-hook at [webHookUrl]
     */
    fun sendToWebHook(webHookUrl: String)
}

private class WindowsShellCommandExecutor : ShellCommandExecutor {
    override fun extractTagFromCommitMessages(tag: String, range: CommitRange?): List<String> {
        return emptyList()
    }
    override fun findBuildTags(buildVariants: Set<String>, limitResultCount: Int): List<Tag>? {
        return null
    }
    override fun sendToWebHook(webHookUrl: String, jsonBody: String) = Unit
    override fun sendToWebHook(webHookUrl: String) = Unit
}

private class LinuxShellCommandExecutor(
    private val project: Project
) : ShellCommandExecutor {

    override fun extractTagFromCommitMessages(tag: String, range: CommitRange?): List<String> {
        val gitLogArgument = when {
            range == null -> ""
            range.sha1 == null -> range.sha2
            else -> "${range.sha1}..${range.sha2}"
        }
        return executeInShell(
            "git log $gitLogArgument " +
                "| grep '$tag' || true"
        )
    }

    override fun findBuildTags(buildVariants: Set<String>, limitResultCount: Int): List<Tag>? {
        // 1. Doing tag list filtering in shell to avoid reading all tags every time, need
        // maximum two latest
        // 2. Manual sort by build number is required, because git sometimes
        // messes up tag order, can't rely on it

        // Format is:
        //  * refname-stuff - prints tag name
        //  * objectname - prints tag sha (for annotated tags) or commit sha (for lightweight tags)
        //  * '*objectname' - prints commit sha to which annotated tag points or empty string for lightweight tags
        val format = "\'%(refname:strip=2) %(objectname) %(*objectname)\'"
        val pattern = buildVariants.joinToString(" ") { "\'*-${it}\'" }
        val commandOutput = executeInShell(
            "git tag --list $pattern --format $format" +
                "| awk -F'[.-]' '{print $(NF-1),$0}' " +
                "| sort -nr " +
                "| cut -d\\  -f2- " +
                "| head -n $limitResultCount"
        )
        return commandOutput.mapNotNull { line ->
            tagOutputLineToTag(line).also {
                if (it == null) project.logger.warn("line doesn't contain a valid tag info, ignoring it. Line: $line")
            }
        }.let { if (it.isEmpty()) null else it }
    }

    @Suppress("MagicNumber")
    private fun tagOutputLineToTag(line: String): Tag? {
        val words = line.split(' ')
        words.forEach {
            require(it.isNotBlank()) {
                "expected tag output lines not to contain blank elements, build plugin internal error"
            }
        }
        if (words.size < 2) {
            return null
        }
        // git describe by default outputs *only* closest annotated tag,
        // use this to find out if last one is annotated
        // (also need to suppress errors to /dev/null,
        // because if git describe doesn't find any tags, it fails with fatal error)
        val name = words.first()
        val isLastTagAnnotated = executeInShell("git describe $name 2> /dev/null || true").firstOrNull()?.trim() == name
        val messageFromTag = if (isLastTagAnnotated) {
            executeInShell("git tag -l --format='%(contents)' $name").joinToString("\n", transform = String::trim)
        } else null
        return Tag.Generic(
            name = name,
            // for annotated tags line will contain "name TAG-SHA TARGET-COMMIT-SHA", we need 3rd
            commitSha = if (words.size >= 3) words[2] else words[1],
            message = messageFromTag
        )
    }

    override fun sendToWebHook(webHookUrl: String, jsonBody: String) {
        executeInShell(
            "curl -i -X POST " +
                "-H 'Content-Type: application/json' " +
                "-d $jsonBody $webHookUrl"
        )
    }

    override fun sendToWebHook(webHookUrl: String) {
        project.logger.debug("sending changelog to $webHookUrl")
        executeInShell("curl -sS '$webHookUrl'")
    }

    private fun executeInShell(command: String): List<String> {
        val stdout = ByteArrayOutputStream()
        project.exec {
            it.commandLine("bash", "-c", command)
            it.standardOutput = stdout
        }

        val commandResult = stdout.toString("utf-8").trim()
        stdout.close()

        project.logger.debug("command: $command")
        project.logger.debug("shell output: $commandResult")
        return if (commandResult.isEmpty()) emptyList() else commandResult.lineSequence().map { it.trim() }.toList()
    }
}

fun getCommandExecutor(project: Project): ShellCommandExecutor {
    return if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        WindowsShellCommandExecutor()
    } else {
        LinuxShellCommandExecutor(project)
    }
}
