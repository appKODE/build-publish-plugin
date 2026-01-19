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

fun mustApplyFoundationPluginMessage(): String {
    return """
        
        |============================================================
        |                 PLUGIN CONFIGURATION ERROR   
        |============================================================
        | The ClickUp plugin requires the BuildPublishFoundationPlugin
        | to be applied first.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        |     id("ru.kode.android.build-publish-novo.firebase") version "<version>"
        | }
        |
        | Make sure to replace <version> with the correct version number.
        |============================================================
        """.trimIndent()
}