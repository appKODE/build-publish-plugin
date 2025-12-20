package ru.kode.android.build.publish.plugin.foundation.messages

import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.EXTENSION_NAME
import java.io.File

fun outputConfigShouldBeDefinedMessage(): String {
    return """
        
        |============================================================
        |             🚨 MISSING OUTPUT CONFIGURATION 🚨
        |============================================================
        | The output configuration has not been defined.
        |
        | REQUIRED ACTION:
        |  1. Ensure you have properly configured the output section
        |  2. Verify your build script includes all required properties
        |
        | Example configuration:
        |
        |  $EXTENSION_NAME {
        |    output {
        |       // Your output configuration here
        |    }
        |  }
        |============================================================
        """.trimIndent()
}

fun configureExtensionMessage(
    extensionName: String,
    variantName: String,
): String {
    return """
        
        |============================================================
        |               ℹ️ EXTENSION CONFIGURATION ℹ️
        |============================================================
        | Configuring `$extensionName` for `$variantName` 
        | in foundation module.
        |
        | This is an informational message indicating that the
        | extension configuration is being processed.
        |============================================================
        """.trimIndent()
}

fun mustBeUsedWithAndroidMessage(): String {
    return """
        
        |============================================================
        |              🚨 PLUGIN CONFIGURATION ERROR 🚨
        |============================================================
        | This plugin can only be used with Android application 
        | projects.
        |
        | REQUIRED ACTION:
        |  1. Apply the 'com.android.application' plugin in 
        |     your build.gradle.kts:
        |
        | plugins {
        |     id("com.android.application")
        |     // Other plugins...
        | }
        |
        | NOTE: This plugin is not compatible with library projects.
        |============================================================
        """.trimIndent()
}

fun mustBeUsedWithVersionMessage(): String {
    return """
        
        |============================================================
        |      ⚠️ UNSUPPORTED ANDROID GRADLE PLUGIN VERSION ⚠️
        |============================================================
        | This plugin requires Android Gradle Plugin version 7.4 
        | or higher.
        |
        | REQUIRED ACTION:
        |  1. Update your project's build.gradle.kts to use AGP 7.4 
        |     or later:
        |
        |  plugins {
        |      id("com.android.application") version "7.4.0"
        |      // Or a newer version
        |  }
        |
        |  2. Sync your project with Gradle files
        |  3. Clean and rebuild your project
        |============================================================
        """.trimIndent()
}

fun tagNotCreatedMessage(
    buildVariant: String,
    buildTagPattern: String,
): String {
    return """
        
        |============================================================
        |             🚨 BUILD TAG FILE NOT CREATED 🚨
        |============================================================
        | Build tag file not created for '$buildVariant' build variant
        | and no stub tag was used because 'useStubsForTagAsFallback' 
        | is false.
        |
        | POSSIBLE REASONS:
        |  1. The pattern '$buildTagPattern' is incorrect
        |  2. No matching tag exists in the repository
        |  3. The tag exists but wasn't fetched
        |
        | TROUBLESHOOTING:
        |  1. Verify that a tag matching the pattern exists:
        |     git tag -l '$buildTagPattern'
        |  2. If the tag exists but isn't being found, 
              try fetching all tags:
        |     git fetch --all --tags
        |  3. Check the pattern in your build configuration
        |  4. For more details, run with --info flag
        |
        | NOTE: 
        | This is a critical error as other tasks depend on this file.
        |============================================================
        """.trimIndent()
}

fun usingStabMessage(
    buildVariant: String,
    buildTagPattern: String,
): String {
    return """
        
        |============================================================
        |           ℹ️ USING STUB TAG FOR BUILD VARIANT ℹ️
        |============================================================
        | Using stub tag for build variant: $buildVariant
        |
        | REASON:
        | No valid tag was found using pattern: $buildTagPattern
        |
        | NEXT STEPS:
        |  1. Verify your tag pattern is correct
        |  2. Check if tags exist in your repository
        |  3. Ensure you have the latest tags: git fetch --all --tags
        |============================================================
        """.trimIndent()
}

fun invalidTagMessage(
    buildTag: Tag.Build,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |             🚨 INVALID BUILD TAG DETECTED 🚨
        |============================================================
        | Invalid build tag '${buildTag.name}' for '$buildVariant' 
        | variant.
        | 
        | Detected build number: ${buildTag.buildNumber}, 
        | expected >= $DEFAULT_VERSION_CODE
        |
        | IMPORTANT:
        |  1. According to Google Play requirements, every Android  
        |     build must have a positive (greater than 0) and  
        |     incrementing version code. 
        |  2. A tag producing a non-positive or reset build number  
        |     cannot be used for release builds.
        |
        | REQUIRED ACTION:
        |  1. Ensure the tag encodes a valid build number 
        |      (>= $DEFAULT_VERSION_CODE)
        |  2. Delete and recreate the incorrect tag:
        |    git tag -d ${buildTag.name}
        |    git push origin :refs/tags/${buildTag.name}
        |    git tag <correct_tag>
        |    git push origin <correct_tag>
        |  3. Re-run the build after correcting the tag
        |
        | NOTE: 
        | Make sure to replace <correct_tag> with a properly 
        | formatted tag
        |============================================================
        """.trimIndent()
}

fun validBuildTagFoundMessage(
    buildTag: Tag.Build,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |                ✅ VALID BUILD TAG FOUND ✅
        |============================================================
        | Valid build tag found for '$buildVariant' variant:
        |
        | Tag: ${buildTag.name}
        | Build Number: ${buildTag.buildNumber}
        |
        | Generating tag build file...
        |============================================================
        """.trimIndent()
}

fun renameApkMessage(
    inputFile: File,
    targetOutputFileName: String,
    outputDir: File,
): String {
    return """
        
        |============================================================
        |                🔄 RENAMING ARTIFACT FILE 🔄
        |============================================================
        | Renaming APK file:
        |
        | Source: ${inputFile.name}
        | Target: $targetOutputFileName
        | Directory: ${outputDir.absolutePath}
        |
        | This step ensures the output file follows the 
        | expected naming convention.
        |============================================================
        """.trimIndent()
}

fun noChangedDetectedSinceStartMessage(): String {
    return """
        
        |============================================================
        |                 ℹ️ NO CHANGES DETECTED ℹ️
        |============================================================
        | No changes detected since the start of the repository 
        | history.
        |
        | POSSIBLE REASONS:
        |  1. This is the first commit in the repository
        |  2. The repository was just initialized
        |
        | NEXT STEPS:
        |  1. Verify your repository has the expected commits
        |  2. Check your Git history: git log --oneline
        |============================================================
        """.trimIndent().trim()
}

fun noChangesDetectedSinceBuildMessage(previousBuildName: String): String {
    return """
        
        |============================================================
        |                 ℹ️ NO CHANGES DETECTED ℹ️
        |============================================================
        | No changes detected since previous build $previousBuildName.
        |
        | POSSIBLE REASONS:
        |  1. No code changes were made since the last build
        |  2. The build configuration has not changed
        |
        | NEXT STEPS:
        |  1. Verify your code changes are committed and pushed
        |  2. Check your build configuration for any changes
        |============================================================
        """.trimIndent().trim()
}

fun changelogGeneratedMessage(
    buildTagPattern: String,
    currentBuildTag: Tag.Build,
): String {
    return """
        
        |============================================================
        |           ✅ CHANGELOG GENERATED SUCCESSFULLY ✅
        |============================================================
        | Changelog has been generated for build tag: 
        | ${currentBuildTag.name}
        |
        | BUILD DETAILS:
        | - Build Tag: ${currentBuildTag.name}
        | - Build Number: ${currentBuildTag.buildNumber}
        | - Tag Pattern: $buildTagPattern
        |
        | The changelog will be included in the build output.
        |============================================================
        """.trimIndent()
}

fun changelogNotGeneratedMessage(
    buildTagPattern: String,
    currentBuildTag: Tag.Build,
): String {
    return """
        
        |============================================================
        |               🚨 CHANGELOG NOT GENERATED 🚨
        |============================================================
        | Changelog is not generated for pattern '$buildTagPattern' 
        | and build tag '$currentBuildTag'.
        |
        | POSSIBLE REASONS:
        |  1. No changes were detected since the last build
        |  2. The build configuration has not changed
        |
        | NEXT STEPS:
        |  1. Verify your code changes are committed and pushed
        |  2. Check your build configuration for any changes
        |============================================================
        """.trimIndent()
}

fun noChangesChangelogMessage(currentBuildTag: Tag.Build): String {
    return """
        
        |============================================================
        |               ℹ️ NO CHANGES IN CHANGELOG ℹ️
        |============================================================
        | No changes detected in the changelog for build tag: 
        | ${currentBuildTag.name}
        |
        | BUILD DETAILS:
        | - Build Tag: ${currentBuildTag.name}
        | - Build Number: ${currentBuildTag.buildNumber}
        |
        | The changelog file will not be created as there are 
        | no changes to document.
        |
        | NOTE: 
        | This is normal if no code changes were made between builds.
        |============================================================
        """.trimIndent()
}
