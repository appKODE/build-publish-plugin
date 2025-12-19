package ru.kode.android.build.publish.plugin.firebase.messages

fun appDistributionConfigNotFoundMessage(buildVariantName: String): String {
    return """
        |
        |============================================================
        |     ⚠️ FIREBASE DISTRIBUTION CONFIG NOT FOUND ⚠️
        |============================================================
        | Firebase App Distribution configuration is missing for build variant:
        | • $buildVariantName
        |
        |============================================================
    """.trimIndent()
}

fun needToProvideDistributionConfigMessage(buildVariantName: String): String {
    return """
        |
        |============================================================
        |     ⚠️ MISSING FIREBASE DISTRIBUTION CONFIGURATION ⚠️
        |============================================================
        | No distribution configuration found for build variant:
        | • $buildVariantName
        |
        | REQUIRED ACTION:
        | 1. Add the distribution configuration for $buildVariantName
        | 2. Include at minimum the Firebase App ID
        |
        | EXAMPLE CONFIGURATION:
        | firebase {
        |     distribution {
        |         $buildVariantName {
        |             appId = "your:firebase:app:id"
        |             // Additional optional parameters:
        |             // testers = listOf("tester@example.com")
        |             // releaseNotes = "Bug fixes and improvements"
        |         }
        |     }
        | }
        |============================================================
    """.trimIndent()
}
