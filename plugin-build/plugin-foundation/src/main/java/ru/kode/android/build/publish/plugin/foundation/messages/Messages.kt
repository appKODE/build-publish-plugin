package ru.kode.android.build.publish.plugin.foundation.messages

import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy
import ru.kode.android.build.publish.plugin.foundation.EXTENSION_NAME
import java.io.File

fun formDefaultVersionCodeMessage(buildVariant: BuildVariant): String {
    return """
        
        |============================================================
        |             FORMING DEFAULT VERSION CODE   
        |============================================================
        | A default versionCode is being generated
        | for the specified build variant.
        |
        |  • Build variant: $buildVariant
        |
        | The default versionCode will be used when
        | no tag-based or custom strategy is applied.
        |============================================================
        """.trimIndent()
}

fun formNullVersionCodeMessage(buildVariant: BuildVariant): String {
    return """
        
        |============================================================
        |               FORMING NULL VERSION CODE   
        |============================================================
        | No versionCode will be assigned for the
        | specified build variant.
        |
        |  • Build variant: $buildVariant
        |
        | This typically indicates that versionCode
        | resolution has been explicitly disabled.
        |============================================================
        """.trimIndent()
}

fun formDefaultVersionNameMessage(buildVariant: BuildVariant): String {
    return """
        
        |============================================================
        |             FORMING DEFAULT VERSION NAME   
        |============================================================
        | A default versionName is being generated
        | for the specified build variant.
        |
        |  • Build variant: $buildVariant
        |
        | The default versionName will be used when
        | no tag-based or custom strategy is applied.
        |============================================================
        """.trimIndent()
}

fun formNullVersionNameMessage(buildVariant: BuildVariant): String {
    return """
        
        |============================================================
        |               FORMING NULL VERSION NAME   
        |============================================================
        | No versionName will be assigned for the
        | specified build variant.
        |
        |  • Build variant: $buildVariant
        |
        | This typically indicates that versionName
        | resolution has been explicitly disabled.
        |============================================================
        """.trimIndent()
}

fun formRichVersionNameMessage(
    buildVariant: BuildVariant,
    tag: Tag.Build?,
): String {
    return """
        
        |============================================================
        |                FORMING RICH VERSION NAME   
        |============================================================
        | A rich versionName is being generated using
        | the following inputs:
        |
        |  • Build variant: $buildVariant
        |  • Build tag: $tag
        |
        | The resulting versionName will incorporate
        | variant- and tag-based metadata.
        |============================================================
        """.trimIndent()
}

fun formRichVersionCodeMessage(
    buildVariant: BuildVariant,
    tag: Tag.Build?,
): String {
    return """
        
        |============================================================
        |              FORMING RICH VERSION CODE   
        |============================================================
        | A rich versionCode is being generated using
        | the following inputs:
        |
        |  • Build variant: $buildVariant
        |  • Build tag: $tag
        |
        | The resulting versionCode will reflect
        | variant- and tag-specific information.
        |============================================================
        """.trimIndent()
}

fun formSimpleApkFileNameMessage(
    buildVariant: BuildVariant,
    baseFileName: String,
): String {
    return """
        
        |============================================================
        |              FORMING SIMPLE APK FILE NAME   
        |============================================================
        | A simple APK file name strategy has been
        | selected.
        |
        |  • Build variant: $buildVariant
        |  • Base file name: $baseFileName
        |
        | No additional metadata will be appended
        | to the final APK file name.
        |============================================================
        """.trimIndent()
}

fun formRichApkFileNameMessage(
    buildVariant: BuildVariant,
    outputFileName: String,
    tag: Tag.Build?,
    baseFileName: String,
): String {
    return """
        
        |============================================================
        |               FORMING RICH APK FILE NAME   
        |============================================================
        | A rich APK file name is being generated using
        | the following inputs:
        |
        |  • Build variant: $buildVariant
        |  • Output file name: $outputFileName
        |  • Build tag: $tag
        |  • Base file name: $baseFileName
        |
        | The final APK name will include versioning
        | and build-specific metadata.
        |============================================================
        """.trimIndent()
}

fun resolvedOutputConfig(
    configName: String,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |               RESOLVED OUTPUT CONFIGURATION   
        |============================================================
        | The output configuration has been successfully
        | resolved and applied.
        |
        |  • Configuration name: $configName
        |  • Build variant: $buildVariant
        |
        | This configuration will be used to generate
        | build artifacts for the specified variant.
        |============================================================
        """.trimIndent()
}

fun resolvedVersionCodeParamsMessage(
    useVersionsFromTag: Boolean,
    useDefaultVersionsAsFallback: Boolean,
    versionCodeStrategy: VersionCodeStrategy,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |             RESOLVED VERSION CODE CONFIGURATION   
        |============================================================
        | The versionCode configuration has been successfully
        | resolved with the following parameters:
        |
        |  • useVersionsFromTag: $useVersionsFromTag
        |  • useDefaultVersionsAsFallback: $useDefaultVersionsAsFallback
        |  • versionCodeStrategy: $versionCodeStrategy
        |  • buildVariant: $buildVariant
        |
        | These values will be used to determine the final
        | versionCode during the build process.
        |============================================================
        """.trimIndent()
}

fun resolvedApkOutputFileNameParamsMessage(
    outputFileName: String,
    useVersionsFromTag: Boolean,
    outputApkNameStrategy: OutputApkNameStrategy,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |          RESOLVED APK OUTPUT NAME CONFIGURATION   
        |============================================================
        | The APK output file name configuration has been
        | successfully resolved with the following parameters:
        |
        |  • outputFileName: $outputFileName
        |  • useVersionsFromTag: $useVersionsFromTag
        |  • outputApkNameStrategy: $outputApkNameStrategy
        |  • buildVariant: $buildVariant
        |
        | These settings determine how the final APK file
        | name will be generated during the build process.
        |============================================================
        """.trimIndent()
}

fun resolvedVersionNameMessage(
    useVersionsFromTag: Boolean,
    useDefaultVersionsAsFallback: Boolean,
    versionNameStrategy: VersionNameStrategy,
    buildVariant: String,
): String {
    return """
        
        |============================================================
        |             RESOLVED VERSION NAME CONFIGURATION   
        |============================================================
        | The versionName configuration has been successfully
        | resolved with the following parameters:
        |
        |  • useVersionsFromTag: $useVersionsFromTag
        |  • useDefaultVersionsAsFallback: $useDefaultVersionsAsFallback
        |  • versionNameStrategy: $versionNameStrategy
        |  • buildVariant: $buildVariant
        |
        | These values will be used to compute the final
        | versionName applied to the build artifact.
        |============================================================
        """.trimIndent()
}

fun outputConfigShouldBeDefinedMessage(): String {
    return """
        
        |============================================================
        |                MISSING OUTPUT CONFIGURATION   
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
        |                  EXTENSION CONFIGURATION   
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
        |                 PLUGIN CONFIGURATION ERROR   
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
        |         UNSUPPORTED ANDROID GRADLE PLUGIN VERSION   
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
        |                BUILD TAG FILE NOT CREATED   
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
        |              USING STUB TAG FOR BUILD VARIANT   
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
        |                INVALID BUILD TAG DETECTED   
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
        |                   VALID BUILD TAG FOUND   
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
        |                    NO CHANGES DETECTED   
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
        |                    NO CHANGES DETECTED   
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
        |              CHANGELOG GENERATED SUCCESSFULLY   
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
        |                  CHANGELOG NOT GENERATED   
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
        |                  NO CHANGES IN CHANGELOG   
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
