package ru.kode.android.build.publish.plugin.firebase.messages

import ru.kode.android.build.publish.plugin.firebase.EXTENSION_NAME

fun appDistributionConfigNotFoundMessage(buildVariantName: String): String {
    return """
        
        |============================================================
        |          FIREBASE DISTRIBUTION CONFIG NOT FOUND   
        |============================================================
        | Firebase App Distribution configuration is missing 
        | for build variant: $buildVariantName
        |
        |============================================================
        """.trimIndent()
}

fun provideDistributionConfigMessage(buildVariantName: String): String {
    return """
        
        |============================================================
        |        MISSING FIREBASE CONFIGURATION
        |============================================================
        | Neither distribution nor remoteConfig configuration found
        | for build variant: $buildVariantName
        |
        | REQUIRED ACTION:
        |  Add a distribution and/or remoteConfig configuration
        |  for $buildVariantName
        |
        | EXAMPLE CONFIGURATION:
        | $EXTENSION_NAME {
        |     distribution {
        |         common { // Or buildVariant($buildVariantName)
        |               // Your distribution settings here
        |         }
        |     }
        |     remoteConfigCommon {
        |         // Your Remote Config audit settings here
        |     }
        | }
        |============================================================
        """.trimIndent()
}
