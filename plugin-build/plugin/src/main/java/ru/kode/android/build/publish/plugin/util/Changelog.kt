package ru.kode.android.build.publish.plugin.util

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.entity.TagRange

internal class Changelog(
    private val commandExecutor: ShellCommandExecutor,
    private val logger: Logger,
    private val messageKey: String,
    private val buildVariants: Set<String>,
) {

    @Suppress("ReturnCount")
    fun buildForRecentBuildTag(defaultValueSupplier: ((TagRange) -> String?)? = null): String? {
        val gitRepository = GitRepository(commandExecutor, buildVariants)
        val buildVariant = gitRepository.findMostRecentBuildTag()?.buildVariant
            .also { if (it == null) logger.warn("failed to build a changelog: no recent build tag") }
            ?: return null
        val tagRange = gitRepository.findBuildTags(buildVariant)
            .also { if (it == null) logger.warn("failed to build a changelog: no build tags") }
            ?: return null
        return build(tagRange) ?: defaultValueSupplier?.invoke(tagRange)
    }

    private fun build(tagRange: TagRange): String? {
        val messageBuilder = StringBuilder().apply {
            val annotatedTagMessage = tagRange.currentBuildTag.message
            if (annotatedTagMessage != null) {
                appendLine(annotatedTagMessage)
                appendLine()
            }
        }

        // it can happen that 2 tags point to the same commit, so no extraction of changelog is necessary
        // (but remember, tags can be annotated - which is taken care of above)
        if (tagRange.currentBuildTag.commitSha != tagRange.previousBuildTag?.commitSha) {
            commandExecutor
                .extractTagFromCommitMessages(messageKey, tagRange.asCommitRange())
                .map { it.replace(Regex("\\s*$messageKey:?\\s*"), "• ") }
                .forEach {
                    messageBuilder.appendLine(it)
                }
        }
        return messageBuilder.toString().takeIf { it.isNotBlank() }
    }
}
