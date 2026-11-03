package ru.kode.android.build.publish.plugin.firebase.remoteconfig

/**
 * Severity of a finding. Any [Fail] finding fails the audit task.
 */
enum class Severity {
    Info,
    Warn,
    Fail,
}
