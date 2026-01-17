package ru.kode.android.build.publish.plugin.foundation.messages

import com.android.build.api.AndroidPluginVersion
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy
import ru.kode.android.build.publish.plugin.foundation.EXTENSION_NAME
import java.io.File

private val VERSION_NUMBER_REGEX = Regex("""\d+(?:\.\d+)+""")

/**
 * Human-readable messages used by the foundation plugin.
 *
 * These helpers return formatted text (mostly multi-line blocks) used for Gradle logging and
 * user-visible diagnostics during version/tag resolution, changelog generation and artifact naming.
 */
fun computedVersionNameMessage(
    buildVariant: BuildVariant,
    versionName: String?,
): String {
    return """
        
        |============================================================
        |             COMPUTED VERSION NAME RESULT
        |============================================================
        | The version name has been successfully
        | computed for the current build.
        |
        |  • Build variant: $buildVariant
        |  • Version name: $versionName
        |
        | This value will be used as the final
        | versionName for the generated artifact.
        |============================================================
        """.trimIndent()
}

/**
 * Message printed after `versionCode` has been computed for a specific [buildVariant].
 */
fun computedVersionCodeMessage(
    buildVariant: BuildVariant,
    code: Int?,
): String {
    return """
        
        |============================================================
        |             COMPUTED VERSION CODE RESULT
        |============================================================
        | The version code has been finalized
        | for the current build configuration.
        |
        |  • Build variant: $buildVariant
        |  • Version code: $code
        |
        | This code uniquely identifies the build
        | for update and distribution purposes.
        |============================================================
        """.trimIndent()
}

/**
 * Message printed after the final APK output file name has been computed.
 */
fun computedApkOutputFileNameMessage(
    buildVariant: BuildVariant,
    apkOutputFileName: String,
): String {
    return """
        
        |============================================================
        |          COMPUTED APK OUTPUT FILE NAME
        |============================================================
        | The final APK output file name has
        | been computed.
        |
        |  • Build variant: $buildVariant
        |  • APK file name: $apkOutputFileName
        |
        | This file name will be used for the
        | generated APK artifact on disk.
        |============================================================
        """.trimIndent()
}

/**
 * Message printed when a default `versionCode` fallback is being used.
 */
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

/**
 * Message printed when no explicit `versionCode` is produced and DSL defaults are used.
 */
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

/**
 * Message printed when a default `versionName` fallback is being used.
 */
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

/**
 * Message printed when no explicit `versionName` is produced and DSL defaults are used.
 */
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

/**
 * Message printed when a rich/tag-based `versionName` is being computed.
 */
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

/**
 * Message printed when a rich/tag-based `versionCode` is being computed.
 */
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

/**
 * Message printed when a simple APK naming strategy (without tag metadata) is used.
 */
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

/**
 * Message printed when a rich APK naming strategy (with tag metadata) is used.
 */
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

/**
 * Message printed when an [OutputConfig] has been resolved for a specific build variant.
 */
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

/**
 * Message printed with resolved inputs used to compute `versionCode`.
 */
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

/**
 * Message printed with resolved inputs used to compute the APK output file name.
 */
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

/**
 * Message printed with resolved inputs used to compute `versionName`.
 */
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

/**
 * Error message shown when no output configuration was provided in the DSL.
 */
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

/**
 * Informational message printed when a [BuildPublishConfigurableExtension] is being configured.
 */
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

/**
 * Error message shown when the plugin is applied to a non-Android application project.
 */
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

/**
 * Error message shown when the Android Gradle Plugin version is below the required minimum.
 */
fun mustBeUsedWithVersionMessage(version: AndroidPluginVersion): String {
    val version = VERSION_NUMBER_REGEX.find(version.toString())?.value
    return """
        
        |============================================================
        |         UNSUPPORTED ANDROID GRADLE PLUGIN VERSION   
        |============================================================
        | This plugin requires Android Gradle Plugin version $version 
        | or higher.
        |
        | REQUIRED ACTION:
        |  1. Update your project's build.gradle.kts to use AGP $version 
        |     or later:
        |
        |  plugins {
        |      id("com.android.application") version "$version"
        |      // Or a newer version
        |  }
        |
        |  2. Sync your project with Gradle files
        |  3. Clean and rebuild your project
        |============================================================
        """.trimIndent()
}

/**
 * Error message printed when the tag snapshot file cannot be created and stub fallback is disabled.
 */
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

/**
 * Informational message printed when a stub tag is used instead of a real Git tag.
 */
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

/**
 * Error message printed when a found build tag is considered invalid for the current variant.
 */
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

/**
 * Informational message printed when a valid build tag is found for a given build variant.
 */
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

/**
 * Message printed when an APK file is being copied/renamed into its final location.
 */
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

/**
 * Default changelog message used when there is no previous build tag.
 */
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

/**
 * Default changelog message used when there are no changes since a previous build tag.
 */
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

/**
 * Message printed when a changelog has been generated successfully.
 */
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

/**
 * Message printed when changelog generation produced no meaningful output.
 */
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

/**
 * Default changelog message written to the output file when there are no changes between builds.
 */
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
