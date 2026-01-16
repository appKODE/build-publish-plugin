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
        |        MISSING FIREBASE DISTRIBUTION CONFIGURATION   
        |============================================================
        | No distribution configuration found 
        | for build variant: $buildVariantName
        |
        | REQUIRED ACTION:
        |  1. Add the distribution configuration for $buildVariantName
        |  2. Include at minimum the Firebase App ID
        |
        | EXAMPLE CONFIGURATION:
        | $EXTENSION_NAME {
        |     distribution {
        |         common { // Or buildVariant($buildVariantName)
        |               // Your distribution settings here
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}
