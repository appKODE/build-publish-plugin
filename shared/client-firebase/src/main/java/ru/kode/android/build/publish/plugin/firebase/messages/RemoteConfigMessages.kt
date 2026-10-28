package ru.kode.android.build.publish.plugin.firebase.messages

fun remoteConfigFailedMessage(failures: List<String>): String {
    return """
        
        |============================================================
        |        FIREBASE REMOTE CONFIG AUDIT FAILED
        |============================================================
        | Findings with Fail severity:
        |
        ${failures.joinToString("\n") { "|  - $it" }}
        |
        | Fix them or lower their severity with
        | severity(<type>, <severity>) in the Remote Config block
        |============================================================
        """.trimMargin()
}
