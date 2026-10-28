package ru.kode.android.build.publish.plugin.firebase.remoteconfig.render

import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Finding
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.FindingType
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.RemoteConfigAudit
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.parseMetadata

private const val MAX_LOCATIONS = 3
private val CAMEL_CASE_BOUNDARY = Regex("(?<=.)(?=[A-Z])")

/**
 * Renders the audit as Markdown. Keys are wrapped in backticks so messengers do not turn
 * `snake_case_keys` into italics.
 */
fun RemoteConfigAudit.toMarkdown(): String =
    buildString {
        appendLine("# Remote Config audit: $currentTag")
        appendLine()
        appendLine("Previous release: ${previousTag ?: "none"}, template version: ${templateVersion ?: "unknown"}")
        notes.forEach { appendLine("> $it") }
        val byType = items.groupBy { it.finding.type }
        FindingType.entries.forEach { type ->
            val group = byType[type] ?: return@forEach
            appendLine()
            appendLine("## ${type.title()} (${group.size})")
            group.forEach { item -> appendLine("- [${item.severity}] ${item.finding.line()}") }
        }
        if (items.isEmpty()) {
            appendLine()
            appendLine("No findings.")
        }
    }

internal fun FindingType.title(): String {
    val words = name.replace(CAMEL_CASE_BOUNDARY, " ").lowercase()
    return words.replaceFirstChar { it.uppercase() }
}

internal fun Finding.line(): String =
    when (this) {
        is Finding.AddedInRelease -> "`$key` ${occurrences.locations()}"
        is Finding.RemovedInRelease -> "`$key` ${occurrences.locations()}"
        is Finding.MissingInRemote -> "`$key` ${occurrences.locations()}"
        is Finding.UnusedInRemote ->
            "`$key`" + parseMetadata(description).owner.ownerSuffix() + description?.let { " — $it" }.orEmpty()
        is Finding.TargetsRelease -> "`$key` = `$value` if `$condition` ($result): `$expression`"
        is Finding.ChangedSinceRelease ->
            "v$versionNumber ${updateTime.orEmpty()} by ${updateUser ?: "unknown"}" +
                description?.let { " ($it)" }.orEmpty() +
                ": " + changedKeys.joinToString { "`$it`" }.ifEmpty { "no parameter changes" }
        is Finding.PotentiallyStale -> "`$key`" + owner.ownerSuffix() + ": $reason"
        is Finding.DefaultsMismatch ->
            "`$key` in-app `${inAppValue ?: "none"}`" + path?.let { " ($it:$line)" }.orEmpty() +
                ", Firebase `${remoteValue ?: "<in-app default>"}`"
    }

private fun String?.ownerSuffix() = this?.let { " (owner $it)" }.orEmpty()

private fun List<Any>.locations(): String {
    val more = if (size > MAX_LOCATIONS) ", …" else ""
    return "(" + take(MAX_LOCATIONS).joinToString() + more + ")"
}
