package ru.kode.android.build.publish.plugin.core.git

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.TagRange

class ChangelogBuilder(
    private val gitRepository: GitRepository,
    private val logger: Logger?,
) {
    @Suppress("ReturnCount")
    fun buildForBuildTag(
        messageKey: String,
        buildTag: Tag.Build,
        buildTagPattern: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null,
    ): String? {
        val buildVariant = buildTag.buildVariant
        return buildForBuildVariant(messageKey, buildVariant, buildTagPattern, defaultValueSupplier)
    }

    private fun buildForBuildVariant(
        messageKey: String,
        buildVariant: String,
        buildTagPattern: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null,
    ): String? {
        val tagRange =
            gitRepository.findTagRange(buildVariant, buildTagPattern)
                .also { if (it == null) logger?.warn("failed to build a changelog: no build tags") }
                ?: return null
        return buildChangelog(
            tagRange,
            { gitRepository.markedCommitMessages(messageKey, tagRange) },
        ) ?: defaultValueSupplier?.invoke(tagRange)
    }

    private fun buildChangelog(
        tagRange: TagRange,
        markedCommitMessagesResolver: () -> List<String>,
    ): String? {
        val messageBuilder =
            StringBuilder().apply {
                val annotatedTagMessage = tagRange.currentBuildTag.message
                if (annotatedTagMessage != null) {
                    appendLine("*$annotatedTagMessage*")
                }
            }

        // it can happen that 2 tags point to the same commit, so no extraction of changelog is necessary
        // (but remember, tags can be annotated - which is taken care of above)
        if (tagRange.currentBuildTag.commitSha != tagRange.previousBuildTag?.commitSha) {
            markedCommitMessagesResolver()
                .forEach { messageBuilder.appendLine(it) }
        }
        return messageBuilder.toString().takeIf { it.isNotBlank() }
    }
}
