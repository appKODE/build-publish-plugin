package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import ru.kode.android.build.publish.plugin.core.entity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.firebase.config.KeySourceSpec

/**
 * Resolved inputs of one Remote Config audit run, see
 * [ru.kode.android.build.publish.plugin.firebase.controller.audit].
 *
 * @property snapshot Current and previous release tags; keys are read from their commits.
 * @property appVersion `versionName` used for `app.version` conditions, `null` when unknown.
 * @property appBuild `versionCode` used for `app.build` conditions, `null` when unknown.
 * @property flagLifetimes Days a parameter of each metadata `type=` may live, see [AnalyzerInput.lifetimes].
 */
data class AuditRequest(
    val projectId: String,
    val snapshot: BuildTagSnapshot,
    val keySources: List<KeySourceSpec>,
    val usageSearchIncludes: List<String>,
    val dynamicKeyPrefixes: Set<String>,
    val ignoreKeys: Set<String>,
    val severities: Map<FindingType, Severity>,
    val appVersion: String?,
    val appBuild: String?,
    val flagLifetimes: Map<String, Int>,
    val inAppDefaultsIncludes: List<String>,
)
