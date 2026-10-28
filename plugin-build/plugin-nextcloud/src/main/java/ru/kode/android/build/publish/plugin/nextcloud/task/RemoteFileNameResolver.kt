package ru.kode.android.build.publish.plugin.nextcloud.task

import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import java.io.File

private const val DEFAULT_BUILD_VERSION = "0.0.0"
private const val DEFAULT_FILE_EXTENSION = "bin"
private val NON_SAFE_FILE_CHARS_REGEX = Regex("[^A-Za-z0-9._-]+")

internal data class NextcloudRemoteFileNameContext(
    val baseOutputFileName: String,
    val buildVariantName: String,
    val buildVariantDefaultVersionName: String?,
    val buildTagSnapshotFile: File,
)

internal fun resolveArtifactRemoteFileName(
    context: NextcloudRemoteFileNameContext,
    sourceFile: File,
    compressed: Boolean,
    explicitRemoteFileName: String?,
): String {
    explicitRemoteFileName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    val buildVersion = resolveBuildVersion(context)
    val sourceExtension = sourceFile.extension.takeIf { it.isNotBlank() } ?: DEFAULT_FILE_EXTENSION
    val segments =
        listOf(
            sanitizeSegment(context.baseOutputFileName, "artifact"),
            sanitizeSegment(buildVersion, DEFAULT_BUILD_VERSION),
            sanitizeSegment(context.buildVariantName, "variant"),
        )

    // A compressed APK and bundle of one build would share a name; keep the source type in it.
    return if (compressed) {
        (segments + sanitizeSegment(sourceExtension, DEFAULT_FILE_EXTENSION)).joinToString("-") + ".zip"
    } else {
        segments.joinToString("-") + ".${sanitizeSegment(sourceExtension, DEFAULT_FILE_EXTENSION)}"
    }
}

internal fun resolveChangelogRemoteFileName(
    context: NextcloudRemoteFileNameContext,
    sourceFile: File,
    explicitRemoteFileName: String?,
): String {
    explicitRemoteFileName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    val buildVersion = resolveBuildVersion(context)
    val extension = sourceFile.extension.takeIf { it.isNotBlank() } ?: "md"

    return listOf(
        sanitizeSegment(context.baseOutputFileName, "artifact"),
        sanitizeSegment(buildVersion, DEFAULT_BUILD_VERSION),
        sanitizeSegment(context.buildVariantName, "variant"),
        "changelog",
    ).joinToString("-") + ".${sanitizeSegment(extension, "md")}"
}

private fun resolveBuildVersion(context: NextcloudRemoteFileNameContext): String {
    val buildVersionFromTag =
        runCatching {
            val snapshotFile = context.buildTagSnapshotFile
            if (!snapshotFile.exists() || snapshotFile.length() <= 0L) {
                null
            } else {
                fromJson(snapshotFile).current.buildVersion
            }
        }.getOrNull()

    return buildVersionFromTag
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: context.buildVariantDefaultVersionName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_BUILD_VERSION
}

private fun sanitizeSegment(
    value: String,
    fallback: String,
): String {
    return value
        .trim()
        .replace(NON_SAFE_FILE_CHARS_REGEX, "-")
        .trim('-')
        .ifBlank { fallback }
}
