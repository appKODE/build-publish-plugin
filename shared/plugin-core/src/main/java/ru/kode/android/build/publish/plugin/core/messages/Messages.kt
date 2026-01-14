package ru.kode.android.build.publish.plugin.core.messages

import okhttp3.Request
import org.ajoberstar.grgit.Commit
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.TagRange
import ru.kode.android.build.publish.plugin.core.util.utcDateTime
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import org.ajoberstar.grgit.Tag as GrgitTag

fun buildingChangelogForTagRangeMessage(tagRange: TagRange): String {
    return """
        
        |============================================================
        |                 BUILDING CHANGELOG
        |============================================================
        | Building changelog for tag range
        |
        | Range: ${tagRange.asCommitRange()}
        |
        | Tags:
        |   - Current: ${tagRange.currentBuildTag.name}
        |   - Previous: ${tagRange.previousBuildTag?.name ?: "Not found"}
        |
        | Commit ranges:
        |   - Current: ${tagRange.currentBuildTag.commitSha}
        |   - Previous: ${tagRange.previousBuildTag?.commitSha ?: "Not found"}
        |
        |============================================================
        """.trimMargin()
}

fun cannotCreateHttpProxyMessage(
    host: String?,
    port: String?,
): String {
    return """
        
        |============================================================
        |                 PROXY CONFIGURATION ERROR
        |============================================================
        | Failed to create HTTP proxy
        |
        | Host: ${host ?: "Not specified"}
        | Port: ${port ?: "Not specified"}
        |
        | ACTION REQUIRED:
        |  1. Verify your proxy host and port configuration
        |  2. Ensure the proxy server is reachable
        |============================================================
        """.trimMargin()
}

fun createHttpProxyMessage(
    host: String,
    port: String,
): String {
    return """
        
        |============================================================
        |            HTTP PROXY CONFIGURED SUCCESSFULLY     
        |============================================================
        | Host: $host
        | Port: $port
        |
        | All HTTP traffic will be routed through this proxy
        |============================================================
        """.trimMargin()
}

fun cannotCreateHttpsProxyMessage(
    host: String?,
    port: String?,
): String {
    return """
        
        |============================================================
        |              HTTPS PROXY CONFIGURATION ERROR
        |============================================================
        | Failed to create HTTPS proxy
        |
        | Host: ${host ?: "Not specified"}
        | Port: ${port ?: "Not specified"}
        |
        | ACTION REQUIRED:
        |  1. Verify your HTTPS proxy settings
        |  2. Ensure the proxy supports HTTPS connections
        |  3. Check for any SSL/TLS configuration issues
        |============================================================
        """.trimMargin()
}

fun createHttpsProxyMessage(
    host: String,
    port: String,
): String {
    return """
        
        |============================================================
        |            HTTPS PROXY CONFIGURED SUCCESSFULLY     
        |============================================================
        | Host: $host
        | Port: $port
        |
        | All HTTPS traffic will be routed through this proxy
        |============================================================
        """.trimMargin()
}

fun proxyConnectionFailedMessage(uri: URI?): String {
    return """
        
        |============================================================
        |                  PROXY CONNECTION FAILED   
        |============================================================
        | Failed to connect to proxy server
        |
        | Target URI: ${uri ?: "Not specified"}
        |
        | POSSIBLE CAUSES:
        |  1. Proxy server is not running
        |  2. Network connectivity issues
        |  3. Incorrect proxy configuration
        |  4. Authentication required but not provided
        |
        | ACTION REQUIRED:
        |  1. Verify proxy server is running and accessible
        |  2. Check network connectivity
        |  3. Review proxy configuration
        |============================================================
        """.trimMargin()
}

fun returnAndApplyProxyMessage(
    uri: URI,
    proxyAddress: InetSocketAddress?,
): String {
    val string = if (proxyAddress != null) "proxy" else "direct"
    return """
        
        |============================================================
        |               APPLYING PROXY CONFIGURATION   
        |============================================================
        | Target URI: $uri
        | Proxy: ${proxyAddress ?: "No proxy (direct connection)"}
        |
        | Proceeding with $string connection...
        |============================================================
        """.trimMargin()
}

fun applyProxyAuthMessage(proxyUser: String): String {
    return """
        
        |============================================================
        |               APPLYING PROXY AUTHENTICATION   
        |============================================================
        | Authenticating as: $proxyUser
        |
        | Proxy authentication credentials will be used for the connection
        |============================================================
        """.trimMargin()
}

fun requestingWithoutProxyMessage(request: Request): String {
    return """
        
        |============================================================
        |                     DIRECT CONNECTION   
        |============================================================
        | Sending request without proxy
        |
        | URL: ${request.url}
        | Method: ${request.method}
        |
        | Proceeding with direct connection...
        |============================================================
        """.trimMargin()
}

fun requestingProxyMessage(
    proxy: Proxy?,
    request: Request,
): String {
    return """
        
        |============================================================
        |                      PROXIED REQUEST    
        |============================================================
        | Sending request via proxy
        |
        | Proxy: ${proxy ?: "None"}
        | URL: ${request.url}
        | Method: ${request.method}
        |
        | Request will be routed through the configured proxy
        |============================================================
        """.trimMargin()
}

fun proxyCredsNotSpecified(): String {
    return """
        
        |============================================================
        |                PROXY AUTHENTICATION MISSING     
        |============================================================
        | Proxy authentication credentials are not specified
        |
        | ACTION REQUIRED:
        |  1. Add the following to your gradle.properties file:
        |     systemProp.https.proxyUser=<your_username>
        |     systemProp.https.proxyPassword=<your_password>
        |
        |  2. OR set them as environment variables:
        |     export GRADLE_OPTS=\"-Dhttps.proxyUser=<user> -Dhttps.proxyPassword=<password>\"
        |
        |  3. For security, consider using a credential manager instead of
        |     storing passwords in plain text
        |
        | NOTE: 
        | These credentials will be used to authenticate with 
        | the proxy server
        |============================================================
        """.trimMargin()
}

fun requiredConfigurationNotFoundMessage(
    name: String,
    defaultName: String,
): String {
    return """
        
        |============================================================
        |                    CONFIGURATION ERROR    
        |============================================================
        | Required configuration not found
        |
        | Expected one of these configurations:
        |   - $name
        |   - $defaultName (fallback)
        |
        | POSSIBLE CAUSES:
        |   1. The configuration was not registered in the build script
        |   2. The configuration name is incorrect
        |   3. There are syntax errors in the build script
        |
        | ACTION REQUIRED:
        |   1. Verify the configuration names in your build script
        |   2. Check for any syntax errors
        |   3. Ensure the plugin is applied correctly
        |============================================================
        """.trimMargin()
}

fun failedToBuildChangelogMessage(): String {
    return """
        
        |============================================================
        |                CHANGELOG GENERATION FAILED     
        |============================================================
        | Could not generate changelog
        |
        | REASON: No build tags found in the repository
        |
        | This typically happens when:
        |   - The repository has no tags
        |   - The tag pattern doesn't match any existing tags
        |   - The git history is empty
        |
        | ACTION REQUIRED:
        |   1. Verify that your repository has tags
        |   2. Check if the tag pattern matches your tag format
        |   3. Ensure you have the correct git history
        |============================================================
        """.trimMargin()
}

fun cannotReturnTagMessage(
    previousTagBuildNumber: Int,
    previousTag: GrgitTag,
    previousTagCommit: Commit,
    lastTagBuildNumber: Int,
    lastTag: GrgitTag,
    lastTagCommit: Commit,
): String {
    val isPreviousCommitDateGreater =
        previousTagCommit.utcDateTime()
            .isAfter(lastTagCommit.utcDateTime())
    val isPreviousCommitBuildNumberGreater = previousTagBuildNumber >= lastTagBuildNumber

    val possibleCauses =
        buildList {
            if (isPreviousCommitDateGreater) add("Commit date of previous tag is after the last tag")
            if (isPreviousCommitBuildNumberGreater) add("Build number of previous tag is >= last tag")
            if (!isPreviousCommitDateGreater && !isPreviousCommitBuildNumberGreater) {
                add("Version number was decreased or git history is inconsistent")
            }
        }.mapIndexed { index, cause -> "   ${index + 1}. $cause" }.joinToString("\n")

    return """
        |============================================================
        |             ️   INVALID TAG ORDER DETECTED   ️   
        |============================================================
        | Cannot process tags due to incorrect version order
        |
        | LAST TAG (should be newer):
        |   - Name: ${lastTag.name}
        |   - Commit: ${lastTag.commit.id.take(7)}
        |   - Date: ${previousTagCommit.utcDateTime()}
        |   - Build: $lastTagBuildNumber
        |
        | PREVIOUS TAG (should be older):
        |   - Name: ${previousTag.name}
        |   - Commit: ${previousTag.commit.id.take(7)}
        |   - Date: ${previousTagCommit.utcDateTime()}
        |   - Build: $previousTagBuildNumber
        |
        | POSSIBLE CAUSES:
        |$possibleCauses
        |
        | ACTION REQUIRED:
        |   1. Ensure build numbers always increase with each release
        |   2. Use semantic versioning (e.g., 1.2.3.${previousTagBuildNumber + 1})
        |   3. Verify your git tag history is correct
        |
        | TIP: Version format should be MAJOR.MINOR.PATCH.BUILD where:
        |   - MAJOR: Breaking changes
        |   - MINOR: New features (backward compatible)
        |   - PATCH: Bug fixes (backward compatible, optional)
        |   - BUILD: Incremental build number (must always increase)
        |============================================================
        """.trimMargin()
}

fun findTagsByNameFoundTagsMessage(lastTwoTags: List<GrgitTag>): String {
    val tagsMessage =
        lastTwoTags.joinToString("\n") {
            "|  - ${it.name.padEnd(30)} (${it.commit.id.take(7)})"
        }
    return """
        
        |============================================================
        |            FOUND ${lastTwoTags.size} TAGS BY NAME
        |============================================================
        | The following tags were found in the repository:
        |
        $tagsMessage
        |
        | These tags will be used for version comparison and changelog
        | generation in the build process.
        |============================================================
        """.trimMargin()
}

fun couldNotFindProvidedBuildTagMessage(
    startTag: Tag.Build,
    buildTagRegex: Regex,
    availableTagNames: String,
): String {
    val availableTagsMessage =
        if (availableTagNames.isEmpty()) {
            "None"
        } else {
            availableTagNames.count { it == ',' } + 1
        }
    val noMatchingTagsMessage =
        if (availableTagNames.isNotEmpty()) {
            "|  - " + availableTagNames.split(",").joinToString("\n|  - ")
        } else {
            "|  No matching tags found"
        }
    return """
        
        |============================================================
        |                 ️   BUILD TAG NOT FOUND   ️  
        |============================================================
        | Could not find the specified build tag in git history
        |
        | TAG DETAILS:
        |   - Name: '${startTag.name}'
        |   - Commit: ${startTag.commitSha.take(7)}
        |   - Regex: $buildTagRegex
        |
        | AVAILABLE TAGS ($availableTagsMessage):
        $noMatchingTagsMessage
        |
        | POSSIBLE CAUSES:
        |   1. The tag doesn't exist in the repository
        |   2. The tag doesn't match the expected pattern
        |   3. The repository doesn't have any tags
        |
        | ACTION REQUIRED:
        |   1. Verify the tag exists: git tag | grep '${startTag.name}'
        |   2. Check your tag pattern in build configuration
        |   3. Ensure you have fetched all tags from remote
        |
        | TIP: Use 'git fetch --tags' to ensure all remote tags are available
        |============================================================
        """.trimMargin()
}

fun findTagsByRangeBeforeSearchMessage(startTag: Tag.Build): String {
    return """
        
        |============================================================
        |                     STARTING TAG SEARCH    
        |============================================================
        | Starting tag search from: ${startTag.name}
        |
        | This tag will be used as the starting point for finding
        | the range of commits to include in the changelog.
        |
        | TAG DETAILS:
        |   - Commit: ${startTag.commitSha.take(7)}
        |
        | The search will look for all tags reachable from this point
        | back to the beginning of the repository history.
        |============================================================
        """.trimMargin()
}

fun finTagsByRegexAfterSortingMessage(tags: List<GrgitTag>): String {
    val tagsMessage =
        tags.joinToString("\n") {
            "|  - ${it.name.padEnd(25)} (${it.commit.id.take(7)})"
        }
    return """
        
        |============================================================
        |           TAGS SORTED BY DATE (${tags.size})    
        |============================================================
        | Tags sorted by commit date (newest first):
        |
        $tagsMessage
        |
        | The tags have been sorted by commit date in descending order.
        | This ensures the changelog shows changes from newest to oldest.
        |============================================================
        """.trimMargin()
}

fun findTagsByRegexAfterFilterMessage(
    buildTagRegex: Regex,
    tags: List<GrgitTag>,
): String {
    val tagsMessage =
        if (tags.isNotEmpty()) {
            tags.joinToString("\n") {
                "|  - ${it.name}"
            }
        } else {
            "|  No tags matched the filter pattern"
        }
    return """
        
        |============================================================
        |        TAGS FILTERED BY REGEX (${tags.size} matches)    
        |============================================================
        | Filter pattern: $buildTagRegex
        |
        | MATCHING TAGS:
        $tagsMessage
        |
        | These tags will be used for version detection and changelog
        | generation. Only tags matching the pattern are considered.
        |============================================================
        """.trimMargin()
}

fun findTagsByRegexBeforeFilterMessage(tags: List<GrgitTag>): String {
    val tagsMessage =
        if (tags.isNotEmpty()) {
            tags.joinToString("\n") {
                "|  - ${it.name.padEnd(30)} (${it.commit.id.take(7)})"
            }
        } else {
            "|  No tags found in the repository"
        }
    return """
        
        |============================================================
        |            FOUND ${tags.size} TAGS IN REPOSITORY    
        |============================================================
        | All tags found before applying any filters:
        |
        $tagsMessage
        |
        | These tags were found before applying any version filtering.
        | The next step will filter them based on the configured pattern.
        |============================================================
        """.trimMargin()
}

fun fileCannotBeParsedMessage(file: File): String {
    return """
        
        |============================================================
        |                    FILE PARSING ERROR     
        |============================================================
        | File $file cannot be parsed
        |
        | REASON: The file has wrong data or a different object
        |
        | ACTION REQUIRED:
        |   1. Verify the file contents
        |   2. Check the file format
        |   3. Ensure the file is correctly formatted
        |============================================================
        """.trimMargin()
}

fun Tag.noVariantMessage(buildVariant: String): String {
    return """
        
        |============================================================
        |              ️   BUILD VARIANT NOT FOUND   ️  
        |============================================================
        | The specified build variant was not found
        |
        | Current tag: ${this.name}
        | Available variant: $buildVariant
        |
        | POSSIBLE CAUSES:
        |   1. The variant name is misspelled
        |   2. The variant hasn't been configured in the build
        |   3. The build types/flavors don't match your configuration
        |
        | ACTION REQUIRED:
        |   1. Check the variant name in your build configuration
        |   2. Verify your build types and product flavors
        |   3. Ensure the variant exists in your build variants
        |============================================================
        """.trimMargin()
}

fun invalidRegexMessage(testRegex: String): String {
    return """
        
        |============================================================
        |                INVALID REGULAR EXPRESSION     
        |============================================================
        | The generated regular expression is invalid
        |
        | Invalid regex: $testRegex
        |
        | POSSIBLE CAUSES:
        |   1. Special characters not properly escaped
        |   2. Unbalanced parentheses or brackets
        |   3. Invalid regex syntax
        |
        | ACTION REQUIRED:
        |   1. Review your tag pattern
        |   2. Check for special characters that need escaping
        |   3. Verify the regex syntax is correct
        |
        | TIP: Use online regex testers to validate your pattern
        |============================================================
        """.trimMargin()
}

fun tagPatterMustContainVariantNameMessage(group: String): String {
    return """
        
        |============================================================
        |                ️   INVALID TAG PATTERN   ️  
        |============================================================
        | The tag pattern must include the variant name
        |
        | Current pattern group: $group
        |
        | ACTION REQUIRED:
        |  1. Update your tag pattern to include the variant name
        |  2. Example: 'v{versionName}.{buildNumber}-{variant}'
        |
        | Current configuration will not work as expected!
        |============================================================
        """.trimMargin()
}

fun tagPatternMustContainVersionGroupMessage(regexPart: String): String {
    return """
        
        |============================================================
        |             ️   INVALID TAG VERSION PATTERN   ️  
        |============================================================
        | The tag pattern must include a version group
        |
        | Current pattern: $regexPart
        |
        | ACTION REQUIRED:
        |  1. Update your tag pattern to include a version group
        |  2. Example patterns:
        |     - 'v{version}'
        |     - '{version}-release'
        |     - 'v{major}.{minor}.{patch}'
        |
        | Current configuration will not work as expected!
        |============================================================
        """.trimMargin()
}
