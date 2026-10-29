package ru.kode.android.build.publish.plugin.firebase.remoteconfig

/**
 * Kinds of [Finding]s, used to configure severities.
 */
enum class FindingType(val defaultSeverity: Severity) {
    AddedInRelease(Severity.Info),
    RemovedInRelease(Severity.Info),
    MissingInRemote(Severity.Warn),
    UnusedInRemote(Severity.Info),
    TargetsRelease(Severity.Info),
    ChangedSinceRelease(Severity.Info),
    PotentiallyStale(Severity.Warn),
    DefaultsMismatch(Severity.Warn),
}
