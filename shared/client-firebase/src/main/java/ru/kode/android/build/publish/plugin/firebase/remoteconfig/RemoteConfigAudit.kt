package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import kotlinx.serialization.Serializable

/**
 * Result of the audit: what `report.json` contains and `report.md` renders.
 */
@Serializable
data class RemoteConfigAudit(
    val currentTag: String,
    val previousTag: String?,
    val templateVersion: String?,
    val items: List<Item>,
    val notes: List<String>,
) {
    @Serializable
    data class Item(
        val severity: Severity,
        val finding: Finding,
    )
}
