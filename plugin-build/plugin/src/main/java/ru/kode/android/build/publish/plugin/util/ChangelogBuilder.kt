package ru.kode.android.build.publish.plugin.util

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.entity.Tag
import ru.kode.android.build.publish.plugin.git.entity.TagRange

internal class ChangelogBuilder(
    private val gitRepository: GitRepository,
    private val commandExecutor: ShellCommandExecutor,
    private val logger: Logger?,
    private val messageKey: String,
) {

    @Suppress("ReturnCount")
    fun buildForBuildTag(
        buildTag: Tag.Build,
        defaultValueSupplier: ((TagRange) -> String?)? = null
    ): String? {
        val buildVariant = buildTag.buildVariant
        return buildForBuildVariant(buildVariant, defaultValueSupplier)
    }

    private fun buildForBuildVariant(
        buildVariant: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null
    ): String? {
        val tagRange = gitRepository.findTagRange(buildVariant)
            .also { if (it == null) logger?.warn("failed to build a changelog: no build tags") }
            ?: return null
        return tagRange.buildChangelog() ?: defaultValueSupplier?.invoke(tagRange)
    }

    private fun TagRange.buildChangelog(): String? {
        val messageBuilder = StringBuilder().apply {
            val annotatedTagMessage = this@buildChangelog.currentBuildTag.message
            if (annotatedTagMessage != null) {
                appendLine(annotatedTagMessage)
                appendLine()
            }
        }

        // it can happen that 2 tags point to the same commit, so no extraction of changelog is necessary
        // (but remember, tags can be annotated - which is taken care of above)
        if (this.currentBuildTag.commitSha != this.previousBuildTag?.commitSha) {
            commandExecutor
                .extractTagFromCommitMessages(messageKey, this.asCommitRange())
                .map { it.replace(Regex("\\s*$messageKey:?\\s*"), "• ") }
                .forEach {
                    messageBuilder.appendLine(it)
                }
        }
        return messageBuilder.toString().takeIf { it.isNotBlank() }
    }
}
